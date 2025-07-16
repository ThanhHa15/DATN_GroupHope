package com.datn.datn.service;

import java.util.List;

import com.datn.datn.model.ProductSpecification;

public interface ProductSpecificationService {
    List<ProductSpecification> findAll();
    List<ProductSpecification> getSpecificationsByProductId(Integer productid);
    void saveSpecification(ProductSpecification spec);
    void deleteSpecification(Integer specId);
    void updateSpecificationValue(Integer specId, String newValue);
}