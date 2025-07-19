package com.datn.datn.service;

import java.util.List;

import com.datn.datn.model.Vouchers;

public interface VoucherService {
    List<Vouchers> findAll();
    Vouchers findById(Long id);
    Vouchers save(Vouchers voucher);
    void delete(Long id);
    boolean existsByCode(String code);
}
