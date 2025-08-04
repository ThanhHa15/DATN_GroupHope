package com.datn.datn.service.impl;

import com.datn.datn.model.Member;
import com.datn.datn.model.Order;
import com.datn.datn.repository.OrderRepository;
import com.datn.datn.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Override
    public Order save(Order order) {
        return orderRepository.save(order);
    }

    @Override
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    @Override
    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    @Override
    public void deleteById(Long id) {
        orderRepository.deleteById(id);
    }
   
   @Override
    public List<Order> findByMemberAndOrderDateAfter(Member member, LocalDateTime date) {
        return orderRepository.findByMemberAndOrderDateAfter(member, date);
    }
    @Override
    public List<Order> findLatestOrdersByMember(Member member) {
        return orderRepository.findByMemberOrderByOrderDateDesc(member);
    }

    @Override
    public List<String> getMemberAddresses(Long memberId) {
        return orderRepository.findDistinctAddressesByMemberId(memberId);
    }
}