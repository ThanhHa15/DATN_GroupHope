package com.datn.datn.service;

import com.datn.datn.model.Category;

import java.util.List;

public interface CategoryService {

    List<Category> getAll();

    Category getById(Integer id);

    Category save(Category category);

    void delete(Integer id);
}
