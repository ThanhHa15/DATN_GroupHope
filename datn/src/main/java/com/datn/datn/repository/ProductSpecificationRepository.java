package com.datn.datn.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.datn.datn.model.ProductSpecification;

import java.util.List;

public interface ProductSpecificationRepository extends JpaRepository<ProductSpecification, Integer> {
    List<ProductSpecification> findByProduct_ProductID(Integer productId);
}
