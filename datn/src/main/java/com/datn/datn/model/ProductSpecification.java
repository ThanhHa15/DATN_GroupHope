package com.datn.datn.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "product_specifications")
public class ProductSpecification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer specId;

   @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)  // cột khóa ngoại trong bảng product_specifications
    private Product product;


    @Column(nullable = false)
    private String specKey;

    @Column(nullable = false)
    private String specValue;
}
