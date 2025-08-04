package com.datn.datn.service;

import java.util.List;

import com.datn.datn.model.Order;
import com.datn.datn.model.OrderDetail;

public interface OrderDetailService {
    OrderDetail save(OrderDetail orderDetail);
    List<OrderDetail> saveAll(List<OrderDetail> orderDetails);

    List<OrderDetail> findByOrder(Order order);
}