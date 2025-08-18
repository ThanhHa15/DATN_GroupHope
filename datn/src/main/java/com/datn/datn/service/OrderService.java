package com.datn.datn.service;

import com.datn.datn.model.Member;
import com.datn.datn.model.Order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface OrderService {

    Order save(Order order);

    Page<Order> findAll(Specification<Order> spec, Pageable pageable);

    Optional<Order> findById(Long id);

    void deleteById(Long id);

    List<Order> findByMemberAndOrderDateAfter(Member member, LocalDateTime date);

    List<Order> findLatestOrdersByMember(Member member);

    List<String> getMemberAddresses(Long memberId);

    List<Order> getOrdersByMemberId(Long memberId);

    String generateOrderId();

    void cancelOrder(Long orderId, String reason);

    void requestReturn(Long orderId, String reason, String returnMethod, List<String> imageUrls);

    Order getOrderById(Long orderId);

    Page<Order> getOrdersByMember(Long memberId, Pageable pageable);

    Page<Order> searchOrdersByMember(Long memberId, String keyword, Pageable pageable);
}
