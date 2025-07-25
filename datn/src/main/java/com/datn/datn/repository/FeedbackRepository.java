package com.datn.datn.repository;

import com.datn.datn.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    @Query("SELECT f FROM Feedback f WHERE f.reviewId = :reviewId ORDER BY f.createdAt DESC")
    List<Feedback> findByReviewId(@Param("reviewId") Long reviewId);
}
