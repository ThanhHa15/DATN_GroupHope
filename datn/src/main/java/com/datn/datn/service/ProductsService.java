package com.datn.datn.service;

import java.io.IOException;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.datn.datn.model.Categories;
import com.datn.datn.model.Products;

public interface ProductsService {
    void add(Products products);

    void delete(Integer id);

    void update(Products products);

    Products findById(Integer id);

    Iterable<Products> findAll();

    Products findByName(String name);

    Page<Products> findAll(Pageable pageable);

    Page<Products> search(String keyword, Pageable pageable);
    
    boolean isNameExists(String name);

    Page<Products> findByNameContaining(String name, Pageable pageable);

    void addProductWithImage(Products products, MultipartFile imageFile) throws IOException;

    void editProductWithImage(Products products, MultipartFile imageFile) throws IOException;

    List<Products> getTop6BestSellingProducts();

    List<Products> getTop6NewestProducts();
    
    List<Products> findRelatedProducts(Integer categoryId, Integer excludeProductId, int limit);
    List<Products> findByCategory(Categories category);
}

