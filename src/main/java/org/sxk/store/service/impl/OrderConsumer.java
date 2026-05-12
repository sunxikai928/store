package org.sxk.store.service.impl;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.sxk.store.config.RabbitMQConfig;
import org.sxk.store.dto.OrderDelayMessageDTO;
import org.sxk.store.dto.OrderMessageDTO;
import org.sxk.store.entity.OrderItem;
import org.sxk.store.entity.Orders;
import org.sxk.store.enums.MessageStatus;
import org.sxk.store.enums.OrderStatus;
import org.sxk.store.mapper.OrderItemMapper;
import org.sxk.store.mapper.OrderMapper;
import org.sxk.store.service.InventoryService;
import org.sxk.store.service.MessageRecordService;
import org.sxk.store.service.ProductService;
import org.sxk.store.service.RedisStockService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OrderConsumer {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final InventoryService inventoryService;
    private final ProductService productService;
    private final RedisStockService redisStockService;
    private final MessageRecordService messageRecordService;
    private final RabbitTemplate rabbitTemplate;

    public OrderConsumer(OrderMapper orderMapper, OrderItemMapper orderItemMapper,
                         InventoryService inventoryService, ProductService productService,
                         RedisStockService redisStockService, MessageRecordService messageRecordService,
                         RabbitTemplate rabbitTemplate) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.inventoryService = inventoryService;
        this.productService = productService;
        this.redisStockService = redisStockService;
        this.messageRecordService = messageRecordService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE)
    @Transactional(rollbackFor = Exception.class)
    public void handleOrderMessage(OrderMessageDTO message, Channel channel, Message amqpMessage) {
        Long userId = message.getUserId();
        String orderNo = message.getOrderNo();
        Long messageId = message.getMessageId();
        List<OrderMessageDTO.OrderItemDTO> itemDTOs = message.getItems();
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();

        log.info("Received order message for user {} with orderNo {} and {} items, messageId: {}", userId, orderNo, itemDTOs.size(), messageId);

        try {
            Orders existingOrder = orderMapper.findByOrderNo(orderNo);
            if (existingOrder != null) {
                log.info("Order already exists, skipping duplicate message. orderNo: {}", orderNo);
                if (messageId != null) {
                    messageRecordService.updateStatus(messageId, MessageStatus.CONSUMED_SUCCESS.getCode(), null);
                }
                channel.basicAck(deliveryTag, false);
                return;
            }

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
            order.setOrderNo(orderNo);
            order.setUserId(userId);
            order.setTotalAmount(totalAmount);
            order.setStatusEnum(OrderStatus.PENDING);

            orderMapper.insert(order);

            for (OrderItem item : items) {
                item.setOrderId(order.getId());
            }
            orderItemMapper.insertBatch(items);

            if (messageId != null) {
                messageRecordService.updateStatus(messageId, MessageStatus.CONSUMED_SUCCESS.getCode(), null);
            }

            sendDelayMessage(order.getId(), order.getOrderNo());

            channel.basicAck(deliveryTag, false);
            log.info("Order created successfully via MQ: {}", order.getId());

        } catch (Exception e) {
            log.error("Failed to process order message", e);

            if (itemDTOs != null) {
                for (OrderMessageDTO.OrderItemDTO itemDTO : itemDTOs) {
                    redisStockService.increaseStock(itemDTO.getProductId(), itemDTO.getQuantity());
                }
            }

            if (messageId != null) {
                messageRecordService.handleMessageFailure(messageId, e.getMessage());
            }

            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ex) {
                log.error("Failed to nack message", ex);
            }
        }
    }

    private void sendDelayMessage(Long orderId, String orderNo) {
        try {
            OrderDelayMessageDTO delayMessage = new OrderDelayMessageDTO();
            delayMessage.setOrderId(orderId);
            delayMessage.setOrderNo(orderNo);

            Map<String, Object> headers = new HashMap<>();
            headers.put("x-delay", RabbitMQConfig.ORDER_DELAY_TIME);

            rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_DELAY_EXCHANGE,
                    RabbitMQConfig.ORDER_DELAY_ROUTING_KEY,
                    delayMessage,
                    msg -> {
                        msg.getMessageProperties().getHeaders().putAll(headers);
                        return msg;
                    });

            log.info("Delay message sent for order: {}, orderNo: {}, delay: {}ms",
                    orderId, orderNo, RabbitMQConfig.ORDER_DELAY_TIME);
        } catch (Exception e) {
            log.error("Failed to send delay message for order: {}", orderId, e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_DELAY_QUEUE)
    @Transactional(rollbackFor = Exception.class)
    public void handleOrderDelayMessage(OrderDelayMessageDTO message, Channel channel, Message amqpMessage) {
        Long orderId = message.getOrderId();
        String orderNo = message.getOrderNo();
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();

        log.info("Received delay message for order: {}, orderNo: {}", orderId, orderNo);

        try {
            Orders order = orderMapper.findById(orderId);
            if (order == null) {
                log.warn("Order not found, orderId: {}, orderNo: {}", orderId, orderNo);
                channel.basicAck(deliveryTag, false);
                return;
            }

            if (!OrderStatus.PENDING.equals(order.getStatusEnum())) {
                log.info("Order is not in pending state, skip cancel. orderId: {}, status: {}", orderId, order.getStatusEnum());
                channel.basicAck(deliveryTag, false);
                return;
            }

            log.info("Canceling order due to timeout. orderId: {}, orderNo: {}", orderId, orderNo);

            List<OrderItem> items = orderItemMapper.findByOrderId(orderId);
            for (OrderItem item : items) {
                inventoryService.unlockStock(item.getProductId(), item.getQuantity());

                org.sxk.store.entity.Inventory inventory = inventoryService.getByProductId(item.getProductId());
                if (inventory != null) {
                    redisStockService.setStock(item.getProductId(), inventory.getStock() + inventory.getLockStock());
                }
            }

            order.setStatusEnum(OrderStatus.CANCELED);
            orderMapper.update(order);

            channel.basicAck(deliveryTag, false);
            log.info("Order canceled due to timeout. orderId: {}, orderNo: {}", orderId, orderNo);
        } catch (Exception e) {
            log.error("Failed to cancel order due to timeout. orderId: {}", orderId, e);

            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (Exception ex) {
                log.error("Failed to nack delay message", ex);
            }
        }
    }
}