package com.datn.datn.service.impl;

import com.datn.datn.model.Product;
import com.datn.datn.repository.ProductRepository;
import com.datn.datn.service.ProductService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository repo;

    public ProductServiceImpl(ProductRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<Product> getAll() {
        return repo.findAll();
    }

    @Override
    public Product getById(Integer id) {
        return repo.findById(id).orElse(null);
    }

    @Override
    public Product save(Product product) {
        return repo.save(product);
    }

    @Override
    public void delete(Integer id) {
        repo.deleteById(id);
    }

    @Override
    public void update(Product updatedProduct) {
        Product existing = repo.findById(updatedProduct.getProductID()).orElse(null);
        if (existing != null) {
            existing.setProductName(updatedProduct.getProductName());
            existing.setDescription(updatedProduct.getDescription());
            existing.setCategory(updatedProduct.getCategory());
            existing.setImageUrl(updatedProduct.getImageUrl());
            repo.save(existing);
        }
    }

    @Override
    public List<Product> findProductsWithDiscount() {
        return repo.findProductsWithActiveDiscount();
    }

    

    

}
