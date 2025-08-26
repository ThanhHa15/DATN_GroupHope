package com.datn.datn.service;

import com.datn.datn.dto.BestSellerDTO;
import org.springframework.data.domain.Page;

public interface BestSellerService {
    Page<BestSellerDTO> getBestSellers(String keyword, Integer categoryId, String status, int page, int size);
}