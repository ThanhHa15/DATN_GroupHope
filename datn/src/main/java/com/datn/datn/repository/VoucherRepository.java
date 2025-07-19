package com.datn.datn.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.datn.datn.model.Vouchers;

public interface VoucherRepository extends JpaRepository<Vouchers, Long> {
    Optional<Vouchers> findByCode(String code);

    boolean existsByCode(String code);
}

