package com.datn.datn.service.impl;

import com.datn.datn.model.Product;
import com.datn.datn.repository.ProductRepository;
import com.datn.datn.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Repository
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

    @Override
    public List<Object[]> countProductsByCategory() {
        return repo.countProductsByCategory();
    }

    @Override
    public Page<Product> getAll(Pageable pageable) {
        return repo.findAll(pageable);
    }

    // Tìm sp theo tên
    @Override
    public Page<Product> searchProductsByName(String keyword, Pageable pageable) {
        return repo.findByProductNameContainingIgnoreCase(keyword, pageable);
    }

    // Lấy sản phẩm theo danh mục
    @Override
    public Page<Product> getByCategoryId(Integer categoryId, Pageable pageable) {
        return repo.findByCategory_CategoryID(categoryId, pageable);
    }

    @Override
    public Page<Product> getByStatus(boolean status, Pageable pageable) {
        return repo.findByStatus(status, pageable);
    }
    
    @Override
    public List<Product> getActiveProducts() {
        return repo.findByStatus(true); // Sử dụng phương thức mới không có Pageable
    }

}
