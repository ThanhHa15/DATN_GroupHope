package com.datn.datn.repository;

import com.datn.datn.model.Feedback;
import com.datn.datn.model.Review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
     List<Feedback> findByReview(Review review);
}