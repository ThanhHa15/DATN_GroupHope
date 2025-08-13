package com.datn.datn.service;

import com.datn.datn.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {
    List<Product> getAll();

    Product getById(Integer id);

    Product save(Product product);

    void delete(Integer id);

    void update(Product updatedProduct);

    List<Product> findProductsWithDiscount();

    List<Object[]> countProductsByCategory();

    // Hàm phân trang
    Page<Product> getAll(Pageable pageable);

    // Tìm sp theo tên
    Page<Product> searchProductsByName(String keyword, Pageable pageable);

    // Lấy sản phẩm theo danh mục
    Page<Product> getByCategoryId(Integer categoryId, Pageable pageable);

    Page<Product> getByStatus(boolean status, Pageable pageable); // Cho phân trang
    
    List<Product> getActiveProducts(); // Lấy tất cả sản phẩm active (không phân trang)



}
