package com.datn.datn.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.datn.datn.model.Product;
import com.datn.datn.model.Review;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    @Query("SELECT r FROM Review r WHERE r.product.productID = :productId")
    List<Review> findByProductId(@Param("productId") Integer productId);

    @Query("SELECT r FROM Review r WHERE r.product.productID = :productId AND r.rating = :rating")
    List<Review> findByProductIdAndRating(@Param("productId") Integer productId, @Param("rating") Integer rating);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.product.productID = :productId")
    Double findAverageRatingByProductId(@Param("productId") Integer productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.productID = :productId")
    Long countByProductId(@Param("productId") Integer productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.productID = :productId AND r.rating = :rating")
    Long countByProductIdAndRating(@Param("productId") Integer productId, @Param("rating") Integer rating);

    boolean existsByProductAndUsername(Product product, String username);
}