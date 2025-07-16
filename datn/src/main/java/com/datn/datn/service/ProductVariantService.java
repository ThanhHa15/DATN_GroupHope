package com.datn.datn.service;

import com.datn.datn.model.Product;
import com.datn.datn.model.ProductVariant;

import java.time.LocalDate;
import java.util.List;

public interface ProductVariantService {

    List<ProductVariant> getAll();

    ProductVariant getById(Integer id);

    ProductVariant save(ProductVariant variant);

    void delete(Integer id);

    List<ProductVariant> getRandomProductVariants(int limit);

    List<ProductVariant> findByProductAndStorage(Product product, String storage);

    List<ProductVariant> findUniqueVariantsByProductAndStorage();

    List<ProductVariant> findByProduct(Product product);

    List<ProductVariant> findDiscountedProducts(); // Tuỳ chọn

    void applyDiscountToStorage(Integer productId, String storage, float discount, LocalDate start, LocalDate end);

    List<String> findStoragesByProductId(Integer productId);

    List<ProductVariant> getDiscountedVariants();

    List<ProductVariant> findDiscountedVariants();

    List<ProductVariant> find6UniqueOtherVariants(Integer excludeVariantId, Integer excludeProductId);




}
