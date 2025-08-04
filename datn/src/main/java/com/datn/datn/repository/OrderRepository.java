package com.datn.datn.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.datn.datn.model.Member;
import com.datn.datn.model.Order;
import com.datn.datn.model.Vouchers;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByMemberAndOrderDateAfter(Member member, LocalDateTime date);

    List<Order> findByMemberOrderByOrderDateDesc(Member member);

    @Query("SELECT DISTINCT o.address FROM Order o WHERE o.member.id = :memberId")
    List<String> findDistinctAddressesByMemberId(@Param("memberId") Long memberId);
}
