package com.datn.datn.repository;

import com.datn.datn.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    @Query("SELECT DISTINCT p FROM Product p JOIN p.variants v WHERE v.discount > 0 AND v.discountStart <= CURRENT_DATE AND v.discountEnd >= CURRENT_DATE")
    List<Product> findProductsWithActiveDiscount();

    @Query("SELECT p.category.name, COUNT(p) FROM Product p GROUP BY p.category.name")
    List<Object[]> countProductsByCategory();

    // Hàm phân trang
    Page<Product> findAll(Pageable pageable);

    // timg kiếm sp
    Page<Product> findByProductNameContainingIgnoreCase(String keyword, Pageable pageable);

    // Lấy sản phẩm theo danh mục
    Page<Product> findByCategory_CategoryID(Integer categoryId, Pageable pageable);

    Page<Product> findByStatus(boolean status, Pageable pageable); // Cho phân trang

    List<Product> findByStatus(boolean status); // Cho trường hợp không phân trang

    // Chỉ đếm tổng số sản phẩm active
    @Query("SELECT COUNT(p) FROM Product p WHERE p.status = true")
    long countActiveProducts();

}
