package com.datn.datn.service.impl;

import com.datn.datn.model.Member;
import com.datn.datn.model.Order;
import com.datn.datn.repository.OrderRepository;
import com.datn.datn.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Override
    public Order save(Order order) {
        if (order.getOrderCode() == null || order.getOrderCode().isEmpty()) {
            order.setOrderCode(generateOrderCode());
        }
        return orderRepository.save(order);
    }

    private String generateOrderCode() {
        // Lấy ngày hiện tại dạng yyyyMMdd
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // Đếm số đơn đã tạo trong ngày
        long countToday = orderRepository.countByOrderDateBetween(
                LocalDate.now().atStartOfDay(),
                LocalDate.now().atTime(23, 59, 59));

        // Số thứ tự + 1 và format thành 3 chữ số
        String sequence = String.format("%03d", countToday + 1);

        return "ORD" + datePart + sequence;
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

    @Override
    public List<Order> getOrdersByMemberId(Long memberId) {
        return orderRepository.findByMemberId(memberId);
    }

    @Override
    public String generateOrderId() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'generateOrderId'");
    }
}