package com.datn.datn.controller;

import com.datn.datn.service.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    @Autowired
    private ChatbotService chatbotService;

    // Nhận dữ liệu dạng JSON {"message":"..."}
    @PostMapping("/query")
    public ChatbotService.ChatbotResponse handleQuery(@RequestBody ChatRequest request) {
        return chatbotService.handleCustomerQuery(request.getMessage());
    }

    // Lớp DTO để nhận dữ liệu từ frontend
    public static class ChatRequest {
        private String message;
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    @GetMapping("/open-detail/{variantId}")
public String openDetail(@PathVariable("variantId") Integer variantId) {
    return "redirect:/detail/" + variantId;
}

}
