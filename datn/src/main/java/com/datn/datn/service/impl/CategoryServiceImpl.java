package com.datn.datn.service.impl;

import com.datn.datn.model.Category;
import com.datn.datn.repository.CategoryRepository;
import com.datn.datn.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository repo;

    public CategoryServiceImpl(CategoryRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<Category> getAll() {
        return repo.findAll();
    }

    @Override
    public Category getById(Integer id) {
        return repo.findById(id).orElse(null);
    }

    @Override
    public Category save(Category category) {
        return repo.save(category);
    }

    @Override
    public void delete(Integer id) {
        repo.deleteById(id);
    }
}
