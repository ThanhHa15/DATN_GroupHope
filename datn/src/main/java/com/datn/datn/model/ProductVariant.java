package com.datn.datn.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Product_variants")
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer variantID;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ProductID", nullable = false)
    private Product product;

    @Column(length = 50, nullable = false)
    private String color;

    @Column(length = 20, nullable = false)
    private String storage;
    @Transient
    private List<String> storages;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Integer quantityInStock;

    @Column(columnDefinition = "nvarchar(max)")
    private String imagesno2;

    // ✅ Thêm phần trăm giảm giá
    @Column
    private Float discount;

    // ✅ Thêm ngày bắt đầu giảm giá
    @Column(name = "discount_start")
    private LocalDate discountStart;

    // ✅ Thêm ngày kết thúc giảm giá
    @Column(name = "discount_end")
    private LocalDate discountEnd;

    @Column(name = "discounted_price")
    private BigDecimal discountedPrice;

    @Transient
    public BigDecimal getDiscountedPrice() { // Tính giá đã giảm
        LocalDate today = LocalDate.now();

        if (price == null)
            return null;

        if (discount != null && discountStart != null && discountEnd != null &&
                !today.isBefore(discountStart) && !today.isAfter(discountEnd)) {
            BigDecimal discountRate = BigDecimal.valueOf(discount).divide(BigDecimal.valueOf(100));
            return BigDecimal.valueOf(price).multiply(BigDecimal.ONE.subtract(discountRate));
        }

        return BigDecimal.valueOf(price);
    }

    @Transient
    private Integer productId;

    public Integer getProductId() { // Trả về productId nếu có, nếu không thì lấy từ product
        return productId != null ? productId : (product != null ? product.getProductID() : null);
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

}
