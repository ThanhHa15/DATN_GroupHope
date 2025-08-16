package com.datn.datn.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.datn.datn.model.Member;
import com.datn.datn.model.Order;
import com.datn.datn.model.Vouchers;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByMemberAndOrderDateAfter(Member member, LocalDateTime date);

    List<Order> findByMemberOrderByOrderDateDesc(Member member);

    @Query("SELECT DISTINCT o.address FROM Order o WHERE o.member.id = :memberId")
    List<String> findDistinctAddressesByMemberId(@Param("memberId") Long memberId);

    List<Order> findByMemberId(Long memberId);

    @Query("SELECT COUNT(o) FROM Order o WHERE DATE(o.orderDate) = :date")
    int countByOrderDate(@Param("date") LocalDate date);

    long countByOrderDateBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT o FROM Order o WHERE o.id = :id AND o.member.id = :memberId")
    Optional<Order> findByIdAndMemberId(@Param("id") Long id, @Param("memberId") Long memberId);

    @Query("SELECT o FROM Order o WHERE o.member.id = :memberId AND o.orderStatus = 'Yêu cầu trả hàng'")
    List<Order> findReturnRequestsByMemberId(@Param("memberId") Long memberId);
}
