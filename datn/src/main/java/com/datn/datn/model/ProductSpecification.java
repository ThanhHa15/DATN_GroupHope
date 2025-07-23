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

    @Column(nullable = false)
    private Integer productid;

    @Column(nullable = false)
    private String specKey;

    @Column(nullable = false)
    private String specValue;
}
