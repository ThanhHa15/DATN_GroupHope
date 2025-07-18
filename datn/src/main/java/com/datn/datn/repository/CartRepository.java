package com.datn.datn.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.datn.datn.model.Cart;
import com.datn.datn.model.Member;
import com.datn.datn.model.ProductVariant;

public interface CartRepository extends JpaRepository<Cart, Long> {
    List<Cart> findByMember(Member member);
    Optional<Cart> findByMemberAndVariant(Member member, ProductVariant variant);

    int countByMember(Member member);
    void deleteByMemberAndVariant(Member member, ProductVariant variant);

}