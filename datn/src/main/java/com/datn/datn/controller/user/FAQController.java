package com.datn.datn.controller.user;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.datn.datn.model.FAQSubmission;
import com.datn.datn.repository.FAQSubmissionRepository; // nhớ import repository

@Controller
public class FAQController {

    @Autowired
    private FAQSubmissionRepository faqSubmissionRepository;

    @GetMapping("/faq")
    public String showFAQPage() {
        return "views/user/faq";
    }

    @PostMapping("/faq/submit")
    public String submitFAQ(@RequestParam String fullName,
                            @RequestParam String email,
                            @RequestParam String phone,
                            @RequestParam String content) {
        FAQSubmission faq = new FAQSubmission();
        faq.setFullName(fullName);
        faq.setEmail(email);
        faq.setPhone(phone);
        faq.setContent(content);
        faq.setCreatedAt(LocalDateTime.now());
        // faq.setStatus(0);
        faqSubmissionRepository.save(faq);
        return "redirect:/faq?success";
    }
}