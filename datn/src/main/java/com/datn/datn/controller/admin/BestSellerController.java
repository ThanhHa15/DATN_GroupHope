package com.datn.datn.controller.admin;

import com.datn.datn.dto.BestSellerDTO;
import com.datn.datn.model.Category;
import com.datn.datn.repository.CategoryRepository;
import com.datn.datn.service.BestSellerService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class BestSellerController {

    private final BestSellerService bestSellerService;
    private final CategoryRepository categoryRepository;

    @GetMapping("/bestsellers")
    public String getBestSellers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model, HttpSession session,
            RedirectAttributes redirectAttributes) {
        String role = (String) session.getAttribute("role");
        if (role == null || (!role.equals("ADMIN") && !role.equals("STAFF"))) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập trang này!");
            return "redirect:/access-denied"; // hoặc trả về 1 trang báo lỗi
        }

        // Xử lý các tham số rỗng
        if (keyword != null && keyword.isEmpty()) {
            keyword = null;
        }
        if (status != null && status.isEmpty()) {
            status = null;
        }

        if (page < 0)
            page = 0;

        Page<BestSellerDTO> bestSellers = bestSellerService.getBestSellers(keyword, categoryId, status, page, size);

        // Nếu page vượt quá totalPages, quay về page cuối cùng
        if (page >= bestSellers.getTotalPages() && bestSellers.getTotalPages() > 0) {
            page = bestSellers.getTotalPages() - 1;
            bestSellers = bestSellerService.getBestSellers(keyword, categoryId, status, page, size);
        }

        model.addAttribute("bestSellers", bestSellers.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", bestSellers.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("status", status);

        List<Category> categories = categoryRepository.findAll();
        model.addAttribute("categories", categories);

        if (bestSellers.isEmpty()) {
            model.addAttribute("message", "Không tìm thấy sản phẩm nào.");
        }

        return "formBestsellerProduct";
    }
}