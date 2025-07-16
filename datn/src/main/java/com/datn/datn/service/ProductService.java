package com.datn.datn.service;

import com.datn.datn.model.Product;

import java.util.List;

public interface ProductService {
    List<Product> getAll();

    Product getById(Integer id);

    Product save(Product product);

    void delete(Integer id);

    void update(Product updatedProduct);

    List<Product> findProductsWithDiscount();


    
}
