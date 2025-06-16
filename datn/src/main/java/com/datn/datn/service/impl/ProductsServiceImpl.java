package com.datn.datn.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.datn.datn.model.Categories;
import com.datn.datn.model.Products;
import com.datn.datn.repository.ProductsRepository;
import com.datn.datn.service.ProductsService;


@Service
public class ProductsServiceImpl implements ProductsService {
    @Autowired
    private ProductsRepository productsRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void add(Products products) {
        productsRepository.save(products);
    }

    @Override
    public void delete(Integer id) {
        productsRepository.deleteById(id);
    }

    @Override
    public void update(Products products) {
        productsRepository.save(products);
    }

    @Override
    public Products findById(Integer id) {
        return productsRepository.findById(id).get();
    }

    @Override
    public Iterable<Products> findAll() {
        return productsRepository.findAll();
    }

    @Override
    public Products findByName(String name) {
        return productsRepository.findByName(name);
    }

    @Override
    public Page<Products> findAll(Pageable pageable) {
        return productsRepository.findAll(pageable);
    }

    @Override
    public Page<Products> search(String keyword, Pageable pageable) {
        return productsRepository.search(keyword, pageable);
    }

    @Override
    public boolean isNameExists(String name) {
        return productsRepository.findByName(name) != null;
    }

    @Override
    public Page<Products> findByNameContaining(String name, Pageable pageable) {
        return productsRepository.findByNameContaining(name, pageable);
    }

    @Override
    public void addProductWithImage(Products products, MultipartFile imageFile) throws IOException {
        if (imageFile != null && !imageFile.isEmpty()) {
            String fileName = UUID.randomUUID().toString() + "_" + imageFile.getOriginalFilename();
            
            Path uploadPath = Paths.get(uploadDir);
            
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(imageFile.getInputStream(), filePath);
            
            String relativePath = "images/" + fileName;
            products.setImage(relativePath);
        } else {
            products.setImage("images/default-image.jpg"); 
        }

        productsRepository.save(products);
    }

    @Override
    public void editProductWithImage(Products products, MultipartFile imageFile) throws IOException {
        if (imageFile != null && !imageFile.isEmpty()) {
            String fileName = UUID.randomUUID().toString() + "_" + imageFile.getOriginalFilename();
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(imageFile.getInputStream(), filePath);

            String relativePath = "images/" + fileName;
            products.setImage(relativePath);
        } else {
            // Giữ nguyên ảnh cũ
            Products existingProduct = productsRepository.findById(products.getId()).orElse(null);
            if (existingProduct != null) {
                products.setImage(existingProduct.getImage());
            }
        }

        productsRepository.save(products);
    }

    @Override
    public List<Products> getTop6BestSellingProducts() {
        return productsRepository.findTop6ByOrderBySoldQuantityDesc();
    }

    @Override
    public List<Products> getTop6NewestProducts() {
        return productsRepository.findTop6ByOrderByCreatedAtDesc();
    }
    
    @Override
    public List<Products> findRelatedProducts(Integer categoryId, Integer excludeProductId, int limit) {
        return productsRepository.findByCategoryIdAndIdNot(categoryId, excludeProductId, PageRequest.of(0, limit));
    }
    @Override
    public List<Products> findByCategory(Categories category) {
        return productsRepository.findByCategory(category);
    }
}
