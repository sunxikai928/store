package org.sxk.store.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.sxk.store.dto.PlaceOrderRequest;
import org.sxk.store.entity.OrderItem;
import org.sxk.store.entity.Orders;
import org.sxk.store.entity.Product;
import org.sxk.store.enums.OrderStatus;
import org.sxk.store.enums.ProductStatus;
import org.sxk.store.mapper.OrderItemMapper;
import org.sxk.store.mapper.OrderMapper;
import org.sxk.store.mapper.ProductMapper;
import org.sxk.store.service.InventoryService;
import org.sxk.store.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final InventoryService inventoryService;

    public OrderServiceImpl(OrderMapper orderMapper, OrderItemMapper orderItemMapper, 
                           ProductMapper productMapper, InventoryService inventoryService) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
        this.inventoryService = inventoryService;
    }

    @Override
    public List<Orders> getAllOrders() {
        log.info("Getting all orders");
        return orderMapper.findAll();
    }

    @Override
    public Orders getOrderById(Long id) {
        log.info("Getting order by id: {}", id);
        return orderMapper.findById(id);
    }

    @Override
    public int createOrder(Orders order) {
        log.info("Creating order for user: {}", order.getUserId());
        return orderMapper.insert(order);
    }

    @Override
    public int updateOrder(Orders order) {
        log.info("Updating order: {}", order.getId());
        return orderMapper.update(order);
    }

    @Override
    @Transactional
    public int deleteOrder(Long id) {
        log.info("Deleting order: {}", id);
        orderItemMapper.deleteByOrderId(id);
        return orderMapper.delete(id);
    }

    @Override
    public List<Orders> getOrdersByUserId(Long userId) {
        log.info("Getting orders for user: {}", userId);
        return orderMapper.findByUserId(userId);
    }

    @Override
    @Transactional
    public Long placeOrder(PlaceOrderRequest request) {
        Long userId = request.getUserId();
        List<PlaceOrderRequest.OrderItemDTO> itemDTOs = request.getItems();
        
        log.info("Placing order for user {} with {} items", userId, itemDTOs.size());
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();
        
        for (PlaceOrderRequest.OrderItemDTO itemDTO : itemDTOs) {
            Product product = productMapper.findById(itemDTO.getProductId());
            if (product == null) {
                log.error("Product not found: {}", itemDTO.getProductId());
                throw new RuntimeException("Product not found: " + itemDTO.getProductId());
            }
            
            if (!ProductStatus.ON_SHELF.equals(product.getStatusEnum())) {
                log.error("Product is not available: {}", itemDTO.getProductId());
                throw new RuntimeException("Product is not available: " + product.getName());
            }
            
            OrderItem item = new OrderItem();
            item.setProductId(itemDTO.getProductId());
            item.setQuantity(itemDTO.getQuantity());
            item.setPrice(product.getPrice());
            
            BigDecimal itemAmount = product.getPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity()));
            totalAmount = totalAmount.add(itemAmount);
            
            items.add(item);
        }
        
        for (OrderItem item : items) {
            boolean locked = inventoryService.lockStock(item.getProductId(), item.getQuantity());
            if (!locked) {
                log.error("Failed to lock stock for product: {}", item.getProductId());
                throw new RuntimeException("Insufficient stock for product: " + item.getProductId());
            }
        }
        
        Orders order = new Orders();
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatusEnum(OrderStatus.PENDING);
        
        orderMapper.insert(order);
        
        for (OrderItem item : items) {
            item.setOrderId(order.getId());
        }
        orderItemMapper.insertBatch(items);
        
        log.info("Order created successfully: {}", order.getId());
        return order.getId();
    }

    @Override
    @Transactional
    public boolean payOrder(Long orderId) {
        log.info("Paying order: {}", orderId);
        
        Orders order = orderMapper.findById(orderId);
        if (order == null) {
            log.error("Order not found: {}", orderId);
            return false;
        }
        
        if (!OrderStatus.PENDING.equals(order.getStatusEnum())) {
            log.error("Order is not in pending state: {}", orderId);
            return false;
        }
        
        List<OrderItem> items = orderItemMapper.findByOrderId(orderId);
        for (OrderItem item : items) {
            inventoryService.deductStock(item.getProductId(), item.getQuantity());
        }
        
        order.setStatusEnum(OrderStatus.PAID);
        orderMapper.update(order);
        
        log.info("Order {} paid successfully", orderId);
        return true;
    }

    @Override
    @Transactional
    public boolean cancelOrder(Long orderId) {
        log.info("Canceling order: {}", orderId);
        
        Orders order = orderMapper.findById(orderId);
        if (order == null) {
            log.error("Order not found: {}", orderId);
            return false;
        }
        
        if (!OrderStatus.PENDING.equals(order.getStatusEnum())) {
            log.error("Order cannot be canceled, current status: {}", order.getStatusEnum());
            return false;
        }
        
        List<OrderItem> items = orderItemMapper.findByOrderId(orderId);
        for (OrderItem item : items) {
            inventoryService.unlockStock(item.getProductId(), item.getQuantity());
        }
        
        order.setStatusEnum(OrderStatus.CANCELED);
        orderMapper.update(order);
        
        log.info("Order {} canceled successfully", orderId);
        return true;
    }

    @Override
    public List<OrderItem> getOrderItems(Long orderId) {
        log.info("Getting order items for order: {}", orderId);
        return orderItemMapper.findByOrderId(orderId);
    }
}