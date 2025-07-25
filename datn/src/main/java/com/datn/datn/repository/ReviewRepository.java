package com.datn.datn.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.datn.datn.model.Review;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    @Query(value = "SELECT * FROM reviews WHERE productid = :productId", nativeQuery = true)
    List<Review> findByProductId(@Param("productId") Long productId);

    // Add this new method
    @Query(value = "SELECT * FROM reviews WHERE productid = :productId AND rating = :rating", nativeQuery = true)
    List<Review> findByProductIdAndRating(@Param("productId") Long productId, @Param("rating") Integer rating);

    @Query(value = "SELECT ISNULL(AVG(CAST(rating AS DECIMAL(10,2)), 0) FROM reviews WHERE productid = :productId", 
           nativeQuery = true)
    Double findAverageRatingByProductId(@Param("productId") Long productId);

    @Query(value = "SELECT COUNT() FROM reviews WHERE productid = :productId", nativeQuery = true)
    Long countByProductId(@Param("productId") Long productId);

    @Query(value = "SELECT COUNT() FROM reviews WHERE productid = :productId AND rating = :rating", 
           nativeQuery = true)
    Long countByProductIdAndRating(@Param("productId") Long productId, @Param("rating") Integer rating);

    default double getAverageRatingByProductId(Long productId) {
        Double avg = findAverageRatingByProductId(productId);
        return avg != null ? avg : 0.0;
    }

}
