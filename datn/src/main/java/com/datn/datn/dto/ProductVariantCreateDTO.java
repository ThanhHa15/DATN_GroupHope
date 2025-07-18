package com.datn.datn.dto;

import java.util.List;

import com.datn.datn.model.ProductVariant;

public class ProductVariantCreateDTO {

    private ProductVariant variant; // Entity ProductVariant
    private List<VariantSpecificationDTO> specifications; // danh sách thông số kỹ thuật

    // Getters & Setters
    public ProductVariant getVariant() {
        return variant;
    }

    public void setVariant(ProductVariant variant) {
        this.variant = variant;
    }

    public List<VariantSpecificationDTO> getSpecifications() {
        return specifications;
    }

    public void setSpecifications(List<VariantSpecificationDTO> specifications) {
        this.specifications = specifications;
    }
}
