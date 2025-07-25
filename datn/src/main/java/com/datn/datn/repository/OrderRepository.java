package com.datn.datn.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.datn.datn.model.Order;
import com.datn.datn.model.Vouchers;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
