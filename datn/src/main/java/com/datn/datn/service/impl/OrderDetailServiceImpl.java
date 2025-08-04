package com.datn.datn.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.datn.datn.model.Order;
import com.datn.datn.model.OrderDetail;
import com.datn.datn.repository.OrderDetailRepository;
import com.datn.datn.service.OrderDetailService;

@Service
public class OrderDetailServiceImpl implements OrderDetailService {

    private final OrderDetailRepository orderDetailRepository;

    @Autowired
    public OrderDetailServiceImpl(OrderDetailRepository orderDetailRepository) {
        this.orderDetailRepository = orderDetailRepository;
    }

    @Override
    public OrderDetail save(OrderDetail orderDetail) {
        return orderDetailRepository.save(orderDetail);
    }

    @Override
    public List<OrderDetail> saveAll(List<OrderDetail> orderDetails) {
        return orderDetailRepository.saveAll(orderDetails);
    }
    @Override
    public List<OrderDetail> findByOrder(Order order) {
        return orderDetailRepository.findByOrder(order);
    }
}
