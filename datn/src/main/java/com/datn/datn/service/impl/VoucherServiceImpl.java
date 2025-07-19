package com.datn.datn.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.datn.datn.model.Vouchers;
import com.datn.datn.repository.VoucherRepository;
import com.datn.datn.service.VoucherService;

@Service
public class VoucherServiceImpl implements VoucherService {

    @Autowired
    private VoucherRepository voucherRepository;

    @Override
    public List<Vouchers> findAll() {
        return voucherRepository.findAll();
    }

    @Override
    public Vouchers findById(Long id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher"));
    }

    @Override
    public Vouchers save(Vouchers voucher) {
        return voucherRepository.save(voucher);
    }

    @Override
    public void delete(Long id) {
        voucherRepository.deleteById(id);
    }
    @Override
    public boolean existsByCode(String code) {
        return voucherRepository.existsByCode(code);
    }
}

