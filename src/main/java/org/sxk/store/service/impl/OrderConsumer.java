package org.sxk.store.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.sxk.store.config.RabbitMQConfig;
import org.sxk.store.dto.OrderMessageDTO;
import org.sxk.store.entity.OrderItem;
import org.sxk.store.entity.Orders;
import org.sxk.store.enums.OrderStatus;
import org.sxk.store.mapper.OrderItemMapper;
import org.sxk.store.mapper.OrderMapper;
import org.sxk.store.service.InventoryService;
import org.sxk.store.service.ProductService;
import org.sxk.store.service.RedisStockService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class OrderConsumer {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final InventoryService inventoryService;
    private final ProductService productService;
    private final RedisStockService redisStockService;

    public OrderConsumer(OrderMapper orderMapper, OrderItemMapper orderItemMapper,
                         InventoryService inventoryService, ProductService productService,
                         RedisStockService redisStockService) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.inventoryService = inventoryService;
        this.productService = productService;
        this.redisStockService = redisStockService;
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE)
    @Transactional
    public void handleOrderMessage(OrderMessageDTO message) {
        Long userId = message.getUserId();
        List<OrderMessageDTO.OrderItemDTO> itemDTOs = message.getItems();

        log.info("Received order message for user {} with {} items", userId, itemDTOs.size());

        try {
            BigDecimal totalAmount = BigDecimal.ZERO;
            List<OrderItem> items = new ArrayList<>();

            for (OrderMessageDTO.OrderItemDTO itemDTO : itemDTOs) {
                OrderItem item = new OrderItem();
                item.setProductId(itemDTO.getProductId());
                item.setQuantity(itemDTO.getQuantity());
                item.setPrice(itemDTO.getPrice());

                BigDecimal itemAmount = itemDTO.getPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity()));
                totalAmount = totalAmount.add(itemAmount);

                items.add(item);
            }

            for (OrderItem item : items) {
                boolean locked = inventoryService.lockStock(item.getProductId(), item.getQuantity());
                if (!locked) {
                    log.error("Failed to lock stock in DB for product: {}", item.getProductId());

                    redisStockService.increaseStock(item.getProductId(), item.getQuantity());
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

            log.info("Order created successfully via MQ: {}", order.getId());

        } catch (Exception e) {
            log.error("Failed to process order message", e);

            if (itemDTOs != null) {
                for (OrderMessageDTO.OrderItemDTO itemDTO : itemDTOs) {
                    redisStockService.increaseStock(itemDTO.getProductId(), itemDTO.getQuantity());
                }
            }
        }
    }
}