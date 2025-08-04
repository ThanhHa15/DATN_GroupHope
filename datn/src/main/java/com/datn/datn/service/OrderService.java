package com.datn.datn.service;

import com.datn.datn.model.Member;
import com.datn.datn.model.Order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderService {

    Order save(Order order);

    List<Order> findAll();

    Optional<Order> findById(Long id);

    void deleteById(Long id);

    List<Order> findByMemberAndOrderDateAfter(Member member, LocalDateTime date);
     List<Order> findLatestOrdersByMember(Member member);
}
