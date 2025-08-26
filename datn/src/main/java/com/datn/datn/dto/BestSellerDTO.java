package com.datn.datn.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BestSellerDTO {
    private Integer productId;
    private String productName;
    private String config;
    private String categoryName;
    private BigDecimal price;
    private Long totalSold;
    private String imageUrl;
    private String status;
}
