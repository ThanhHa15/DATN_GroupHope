package com.datn.datn.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Khóa ngoại tới bảng members
    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // Khóa ngoại tới bảng product_variants
    @ManyToOne
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant productVariant;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @Column(name = "address", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String address;

    @Column(name = "note", columnDefinition = "NVARCHAR(500)")
    private String note;

    @Column(name = "total_price", nullable = false)
    private Double totalPrice;

}
