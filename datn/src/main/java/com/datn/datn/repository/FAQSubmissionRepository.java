package com.datn.datn.repository;

import com.datn.datn.model.FAQSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FAQSubmissionRepository extends JpaRepository<FAQSubmission, Long> {
    
    List<FAQSubmission> findByStatusOrderByCreatedAtDesc(String status);
    
    List<FAQSubmission> findAllByOrderByCreatedAtDesc();
}
