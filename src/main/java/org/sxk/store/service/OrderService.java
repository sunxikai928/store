package org.sxk.store.service;

import org.sxk.store.dto.PlaceOrderRequest;
import org.sxk.store.entity.OrderItem;
import org.sxk.store.entity.Orders;

import java.util.List;

public interface OrderService {
    List<Orders> getAllOrders();
    Orders getOrderById(Long id);
    int createOrder(Orders order);
    int updateOrder(Orders order);
    int deleteOrder(Long id);
    List<Orders> getOrdersByUserId(Long userId);
    Long placeOrder(PlaceOrderRequest request);
    boolean payOrder(Long orderId);
    boolean cancelOrder(Long orderId);
    List<OrderItem> getOrderItems(Long orderId);
}