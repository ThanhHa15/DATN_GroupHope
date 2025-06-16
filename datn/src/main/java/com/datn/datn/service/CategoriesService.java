package com.datn.datn.service;


import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.datn.datn.model.Categories;

public interface CategoriesService {
    void add(Categories categories);
    
    void delete(Long id);

    void update(Categories categories);
    
    Categories findById(Long id);

    Iterable<Categories> findAll();
    
    Categories findByName(String name);

    Page<Categories> findAll(Pageable pageable);

    Page<Categories> search(String keyword, Pageable pageable);

    boolean isNameExists(String name);
    
    Page<Categories> findByNameContaining(String name, Pageable pageable);

    List<Categories> findAlla();
    void deleteWithProducts(Long id);
}
