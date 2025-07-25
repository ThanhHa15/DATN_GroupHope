package com.datn.datn.service;

import com.datn.datn.model.Order;

import java.util.List;
import java.util.Optional;

public interface OrderService {

    Order save(Order order);

    List<Order> findAll();

    Optional<Order> findById(Long id);

    void deleteById(Long id);
}
