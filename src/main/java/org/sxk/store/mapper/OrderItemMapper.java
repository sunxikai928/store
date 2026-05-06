package org.sxk.store.mapper;

import org.sxk.store.entity.OrderItem;

import java.util.List;

public interface OrderItemMapper {
    int insert(OrderItem orderItem);
    int insertBatch(List<OrderItem> orderItems);
    List<OrderItem> findByOrderId(Long orderId);
    int deleteByOrderId(Long orderId);
}