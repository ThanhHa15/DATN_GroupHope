package com.datn.datn.repository;


import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.datn.datn.model.Categories;
import com.datn.datn.model.Products;


@Repository
public interface ProductsRepository extends JpaRepository<Products, Integer> {
    Products findByName(String name);

    Products findByNameContaining(String name);

    Page<Products> findByNameContaining(String name, Pageable pageable);

    @Query("SELECT p FROM Products p WHERE p.name LIKE %:keyword%")
    Page<Products> search(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Products p WHERE p.name LIKE %:name% OR p.id = :id")
    Page<Products> searchByNameOrId(@Param("name") String name, @Param("id") Integer id, Pageable pageable);

    @Query("SELECT p FROM Products p ORDER BY p.soldQuantity DESC LIMIT 6")
    List<Products> findTop6ByOrderBySoldQuantityDesc();

    @Query("SELECT p FROM Products p ORDER BY p.createdAt DESC LIMIT 6")
    List<Products> findTop6ByOrderByCreatedAtDesc();
    List<Products> findByCategoryIdAndIdNot(Integer categoryId, Integer excludeProductId, Pageable pageable);
    void deleteByCategoryId(Integer categoryId);

    List<Products> findByCategory(Categories category);
}
