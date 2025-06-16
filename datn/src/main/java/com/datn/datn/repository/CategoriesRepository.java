package com.datn.datn.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable; 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.datn.datn.model.Categories;

@Repository
public interface CategoriesRepository extends JpaRepository<Categories, Long> {
    Categories findByName(String name);

    Page<Categories> findByNameContaining(String name, Pageable pageable);

    @Query("SELECT c FROM Categories c WHERE c.name LIKE %:keyword%")
    Page<Categories> search(@Param("keyword") String keyword, Pageable pageable); 

    @Query("SELECT c FROM Categories c WHERE c.name LIKE %:name% OR c.id = :id")
    Page<Categories> searchByNameOrId(@Param("name") String name, @Param("id") Integer id, Pageable pageable);
}