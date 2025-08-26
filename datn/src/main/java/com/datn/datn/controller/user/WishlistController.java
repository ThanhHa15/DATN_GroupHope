package com.datn.datn.controller.user;

import com.datn.datn.model.Member;
import com.datn.datn.model.ProductVariant;
import com.datn.datn.model.Wishlist;
import com.datn.datn.repository.MemberRepository;
import com.datn.datn.repository.ProductVariantRepository;
import com.datn.datn.repository.WishlistRepository;
import com.datn.datn.service.ProductService;
import com.datn.datn.service.WishlistService;

import org.springframework.ui.Model;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class WishlistController {
    @Autowired
    private ProductService productService;

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleWishlist(@RequestParam int variantId,
            HttpSession session) {
        Member user = (Member) session.getAttribute("loggedInUser");
        Map<String, Object> response = new HashMap<>();

        if (user == null) {
            response.put("message", "Bạn cần đăng nhập để thêm yêu thích!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        ProductVariant variant = productVariantRepository.findById(variantId).orElse(null);
        if (variant != null) {
            boolean added = wishlistService.toggleWishlist(user, variant);
            if (added) {
                response.put("status", "added");
                response.put("message", "Đã thêm vào danh sách yêu thích!");
            } else {
                response.put("status", "removed");
                response.put("message", "Đã xóa khỏi danh sách yêu thích!");
            }
        } else {
            response.put("message", "Sản phẩm không tồn tại!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/wishlist")
    public String showWishlistPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model,
            HttpSession session,
            @ModelAttribute("message") String message) {

        Member user = (Member) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }

        List<ProductVariant> wishlist = wishlistRepository.findWishlistByUserId(user.getId());

        // 👉 Phân trang
        int totalItems = wishlist.size();
        int totalPages = (int) Math.ceil((double) totalItems / size);
        int fromIndex = Math.min((page - 1) * size, totalItems);
        int toIndex = Math.min(fromIndex + size, totalItems);
        List<ProductVariant> pagedWishlist = wishlist.subList(fromIndex, toIndex);

        List<Object[]> counts = productService.countProductsByCategory();
        Map<String, Long> categoryCounts = new HashMap<>();
        for (Object[] row : counts) {
            String categoryName = (String) row[0];
            Long count = (Long) row[1];
            categoryCounts.put(categoryName, count);
        }
        
        model.addAttribute("categoryCounts", categoryCounts);
        model.addAttribute("products", pagedWishlist);
        model.addAttribute("wishlistIds", wishlist.stream()
                .map(ProductVariant::getVariantID)
                .collect(Collectors.toSet()));
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);

        if (message != null && !message.isEmpty()) {
            model.addAttribute("message", message);
        }

        return "views/user/wishlist";
    }

}
