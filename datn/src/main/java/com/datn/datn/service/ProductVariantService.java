package com.datn.datn.service;

import com.datn.datn.model.Product;
import com.datn.datn.model.ProductVariant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    List<ProductVariant> filterByStorage(String[] storages);

    void removeDiscount(Integer variantId);

    void updateDiscount(Integer variantId, float discount, LocalDate start, LocalDate end);

    // Hàm phân trang
    Page<ProductVariant> getAll(Pageable pageable);
    // lọc theo danh mục
    // Page<ProductVariant> getByCategoryId(Integer categoryId, Pageable pageable);

    // Tìm kiếm theo tên và màu
    Page<ProductVariant> searchVariantsByName(String keyword, Pageable pageable);

    Page<ProductVariant> getByProductId(Integer productId, Pageable pageable);

    // Page<ProductVariant> getByStatus(String status, Pageable pageable);
    // Danh mục sản phẩm
    Page<ProductVariant> filterByCategory(Integer categoryId, Pageable pageable);

    // Lấy các phiên bản theo trạng thái
    Page<ProductVariant> filterByStatus(String status, Pageable pageable);

    // Thêm các method mới vào interface ProductVariantService

    // Lọc sản phẩm đang giảm giá với phân trang
    Page<ProductVariant> getDiscountedVariants(Pageable pageable);

    // Lọc sản phẩm đang giảm giá theo từ khóa
    Page<ProductVariant> searchDiscountedVariants(String keyword, Pageable pageable);

    // Lọc sản phẩm đang giảm giá theo danh mục
    Page<ProductVariant> getDiscountedVariantsByCategory(Integer categoryId, Pageable pageable);

    // Lọc sản phẩm đang giảm giá với nhiều điều kiện
    Page<ProductVariant> filterDiscountedVariants(String keyword, Integer categoryId, Pageable pageable);

    Page<ProductVariant> filterVariantsWithMultipleFilters(Integer categoryId, String status, String keyword,
            Pageable pageable);

    Page<ProductVariant> searchVariantsByNameWithFilters(String keyword, Integer categoryId, String status,
            Pageable pageable);
}
