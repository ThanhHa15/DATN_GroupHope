package com.datn.datn.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.datn.datn.model.FAQSubmission;
import com.datn.datn.repository.FAQSubmissionRepository;
import com.datn.datn.service.FAQService;

@Service
public class FAQServiceImpl implements FAQService {
    
    @Autowired
    private FAQSubmissionRepository faqSubmissionRepository;
    
    @Override
    public FAQSubmission saveFAQSubmission(FAQSubmission faqSubmission) {
        return faqSubmissionRepository.save(faqSubmission);
    }
    
    @Override
    public List<FAQSubmission> getAllFAQSubmissions() {
        return faqSubmissionRepository.findAllByOrderByCreatedAtDesc();
    }
    
    @Override
    public List<FAQSubmission> getFAQSubmissionsByStatus(String status) {
        return faqSubmissionRepository.findByStatusOrderByCreatedAtDesc(status);
    }
    
    @Override
    public FAQSubmission getFAQSubmissionById(Long id) {
        return faqSubmissionRepository.findById(id).orElse(null);
    }
    
    @Override
    public void updateFAQSubmissionStatus(Long id, String status) {
        FAQSubmission faqSubmission = getFAQSubmissionById(id);
        if (faqSubmission != null) {
            faqSubmission.setStatus(status);
            faqSubmissionRepository.save(faqSubmission);
        }
    }
}