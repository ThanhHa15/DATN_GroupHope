package com.datn.datn.repository;

import com.datn.datn.model.Review;
import com.datn.datn.model.ReviewLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {
    Long countByReview(Review review);

    boolean existsByReviewAndUsername(Review review, String username);

    void deleteByReviewAndUsername(Review review, String username);

}