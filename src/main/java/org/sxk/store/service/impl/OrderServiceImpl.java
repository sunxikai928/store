package org.sxk.store.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.sxk.store.config.RabbitMQConfig;
import org.sxk.store.dto.OrderMessageDTO;
import org.sxk.store.dto.PlaceOrderRequest;
import org.sxk.store.entity.Inventory;
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
import org.sxk.store.service.ProductService;
import org.sxk.store.service.RedisStockService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
    private final ProductService productService;
    private final RedisStockService redisStockService;
    private final RabbitTemplate rabbitTemplate;

    public OrderServiceImpl(OrderMapper orderMapper, OrderItemMapper orderItemMapper, 
                           ProductMapper productMapper, InventoryService inventoryService,
                           ProductService productService, RedisStockService redisStockService,
                           RabbitTemplate rabbitTemplate) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
        this.inventoryService = inventoryService;
        this.productService = productService;
        this.redisStockService = redisStockService;
        this.rabbitTemplate = rabbitTemplate;
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
    @Transactional(rollbackFor = Exception.class)
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
    public Long placeOrder(PlaceOrderRequest request) {
        Long userId = request.getUserId();
        List<PlaceOrderRequest.OrderItemDTO> itemDTOs = request.getItems();
        
        log.info("Placing order for user {} with {} items", userId, itemDTOs.size());
        
        List<OrderMessageDTO.OrderItemDTO> messageItems = new ArrayList<>();
        
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
            
            OrderMessageDTO.OrderItemDTO messageItem = new OrderMessageDTO.OrderItemDTO();
            messageItem.setProductId(itemDTO.getProductId());
            messageItem.setQuantity(itemDTO.getQuantity());
            messageItem.setProductName(product.getName());
            messageItem.setPrice(product.getPrice());
            
            messageItems.add(messageItem);
        }
        
        List<Long> productIds = messageItems.stream()
                .map(OrderMessageDTO.OrderItemDTO::getProductId)
                .toList();
        List<Integer> quantities = messageItems.stream()
                .map(OrderMessageDTO.OrderItemDTO::getQuantity)
                .toList();
        
        boolean success = redisStockService.decreaseStockBatch(productIds, quantities);
        if (!success) {
            log.error("Failed to decrease stock in Redis for products: {}", productIds);
            throw new RuntimeException("Insufficient stock for one or more products");
        }
        
        OrderMessageDTO message = new OrderMessageDTO();
        message.setUserId(userId);
        message.setItems(messageItems);
        
        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, 
                                      RabbitMQConfig.ORDER_ROUTING_KEY, 
                                      message);
        
        log.info("Order message sent to MQ for user {}", userId);
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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
            
            Inventory inventory = inventoryService.getByProductId(item.getProductId());
            if (inventory != null && inventory.getStock() != null && inventory.getStock() <= 0) {
                log.info("Product {} stock is 0, auto unshelving", item.getProductId());
                productService.unshelfProduct(item.getProductId());
            }
        }
        
        order.setStatusEnum(OrderStatus.PAID);
        orderMapper.update(order);
        
        log.info("Order {} paid successfully", orderId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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
            
            Inventory inventory = inventoryService.getByProductId(item.getProductId());
            if (inventory != null && inventory.getStock() != null) {
                redisStockService.setStock(item.getProductId(), inventory.getStock() + inventory.getLockStock());
            }
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