package org.sxk.store.mapper;

import org.sxk.store.entity.Orders;

import java.util.List;

public interface OrderMapper {
    List<Orders> findAll();
    Orders findById(Long id);
    Orders findByOrderNo(String orderNo);
    int insert(Orders order);
    int update(Orders order);
    int delete(Long id);
    List<Orders> findByUserId(Long userId);
}