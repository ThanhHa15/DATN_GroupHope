package com.datn.datn.repository;

import com.datn.datn.model.Product;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    @Query("SELECT DISTINCT p FROM Product p JOIN p.variants v WHERE v.discount > 0 AND v.discountStart <= CURRENT_DATE AND v.discountEnd >= CURRENT_DATE")
    List<Product> findProductsWithActiveDiscount();

}
