package com.datn.datn.service;

import java.util.List;

import com.datn.datn.model.FAQSubmission;

public interface FAQService {
    
    FAQSubmission saveFAQSubmission(FAQSubmission faqSubmission);
    
    List<FAQSubmission> getAllFAQSubmissions();
    
    List<FAQSubmission> getFAQSubmissionsByStatus(String status);
    
    FAQSubmission getFAQSubmissionById(Long id);
    
    void updateFAQSubmissionStatus(Long id, String status);
}