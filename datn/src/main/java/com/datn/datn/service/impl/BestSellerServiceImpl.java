package com.datn.datn.service.impl;

import com.datn.datn.dto.BestSellerDTO;
import com.datn.datn.repository.BestSellerRepository;
import com.datn.datn.service.BestSellerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BestSellerServiceImpl implements BestSellerService {

    private final BestSellerRepository bestSellerRepository;

    @Override
    public Page<BestSellerDTO> getBestSellers(String keyword, Integer categoryId, String status, int page, int size) {
        Page<Object[]> results = bestSellerRepository.getBestSellingProducts(
                keyword,
                categoryId,
                status,
                PageRequest.of(page, size));

        // DEBUG: In ra thông tin để kiểm tra
        System.out.println("Page: " + page + ", Size: " + size);
        System.out.println("Total elements: " + results.getTotalElements());
        System.out.println("Total pages: " + results.getTotalPages());
        System.out.println("Content size: " + results.getContent().size());

        List<BestSellerDTO> dtoList = results.getContent().stream().map(row -> new BestSellerDTO(
                ((Number) row[0]).intValue(),
                (String) row[1],
                (String) row[2],
                (String) row[3],
                new BigDecimal(((Number) row[4]).toString()),
                ((Number) row[5]).longValue(),
                (String) row[6],
                (String) row[7])).collect(Collectors.toList());

        return new PageImpl<>(dtoList, PageRequest.of(page, size), results.getTotalElements());
    }
}