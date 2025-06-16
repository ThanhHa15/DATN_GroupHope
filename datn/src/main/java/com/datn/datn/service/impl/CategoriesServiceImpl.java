package com.datn.datn.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.datn.datn.model.Categories;
import com.datn.datn.model.Products;
import com.datn.datn.repository.CategoriesRepository;
import com.datn.datn.service.CategoriesService;
import com.datn.datn.service.ProductsService;

@Service
public class CategoriesServiceImpl implements CategoriesService {

    @Autowired
    private CategoriesRepository categoriesRepository;
    @Autowired
    private ProductsService productsService;

    @Override
    public void add(Categories categories) {
        categoriesRepository.save(categories);
    }

    @Override
    public void delete(Long id) {
        categoriesRepository.deleteById(id);
    }

    @Override
    public void update(Categories categories) {
        categoriesRepository.save(categories);
    }

    @Override
    public Categories findById(Long id) {
        return categoriesRepository.findById(id).get();
    }

    @Override
    public Iterable<Categories> findAll() {
        return categoriesRepository.findAll();
    }

    @Override
    public Categories findByName(String name) {
        return categoriesRepository.findByName(name);
    }

    @Override
    public Page<Categories> findAll(Pageable pageable) {
        return categoriesRepository.findAll(pageable);
    }

    @Override
    public Page<Categories> search(String keyword, Pageable pageable) {
        return categoriesRepository.search(keyword, pageable);
    }

    @Override
    public boolean isNameExists(String name) {
        return categoriesRepository.findByName(name) != null;
    }

    @Override
    public Page<Categories> findByNameContaining(String name, Pageable pageable) {
        return categoriesRepository.findByNameContaining(name, pageable);
    }

    @Override
    public List<Categories> findAlla() {
        return categoriesRepository.findAll();
    }
    @Override
    public void deleteWithProducts(Long id) {
        // Lấy danh mục trước
        Categories category = categoriesRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Category not found"));
        
        // Xóa tất cả sản phẩm thuộc danh mục này
        List<Products> products = productsService.findByCategory(category);
        for (Products product : products) {
            productsService.delete(product.getId());
        }
        
        // Sau đó xóa danh mục
        categoriesRepository.deleteById(id);
    }
}
