package com.datn.datn.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Table; // ✅ ĐÚNG với Hibernate 6+

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vouchers")
public class Vouchers {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;

    @Column(name = "discount_percent")
    private int discountPercent;

    @Column(name = "minimum_price")
    private BigDecimal minimumPrice;

    private int quantity;

    @Column(name = "start_date")
    @DateTimeFormat(pattern = "yyyy-MM-dd") // Cho phép Spring hiểu định dạng date từ form
    private LocalDate startDate;

    @Column(name = "end_date")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    // Getters & Setters
}
