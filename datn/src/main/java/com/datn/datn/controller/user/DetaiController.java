package com.datn.datn.controller.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.datn.datn.model.Member;
import com.datn.datn.model.ProductSpecification;
import com.datn.datn.model.ProductVariant;
import com.datn.datn.service.ProductSpecificationService;
import com.datn.datn.service.ProductVariantService;
import com.datn.datn.service.WishlistService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/detail")
public class DetaiController {

        private final ProductVariantService productVariantService;

        private final ProductSpecificationService productSpecificationService;

        private final WishlistService wishlistService;

        public DetaiController(ProductVariantService productVariantService,
                        ProductSpecificationService productSpecificationService,
                        WishlistService wishlistService) {
                this.productVariantService = productVariantService;
                this.productSpecificationService = productSpecificationService;
                this.wishlistService = wishlistService;
        }

        @GetMapping("/{id}")
        public String detail(@PathVariable("id") Integer variantId, 
                           @RequestParam(value = "review", required = false) Boolean showReview,
                           Model model, HttpSession session) {
                ProductVariant variant = productVariantService.getById(variantId);

                if (variant == null) {
                        return "redirect:/error";
                }

                List<ProductVariant> sameStorageVariants = productVariantService.findByProductAndStorage(
                                variant.getProduct(), variant.getStorage());

                List<ProductVariant> variantsByProduct = productVariantService.findByProduct(variant.getProduct());

                Map<String, ProductVariant> uniqueVariantsByStorage = variantsByProduct.stream()
                                .collect(Collectors.toMap(
                                                ProductVariant::getStorage,
                                                pv -> pv,
                                                (existing, replacement) -> existing,
                                                LinkedHashMap::new));

                List<ProductSpecification> specifications = productSpecificationService
                                .getSpecificationsByProductId(variant.getProduct().getProductID());

                List<ProductVariant> allUniqueVariants = productVariantService.findUniqueVariantsByProductAndStorage();

                List<ProductVariant> otherProducts = allUniqueVariants.stream()
                                .filter(pv -> !pv.getProduct().getProductID()
                                                .equals(variant.getProduct().getProductID()))
                                .filter(pv -> pv.getProduct().getCategory().getCategoryID()
                                                .equals(variant.getProduct().getCategory().getCategoryID()))
                                .limit(5)
                                .collect(Collectors.toList());

                // ✅ Thêm wishlist nếu người dùng đã đăng nhập
                Member user = (Member) session.getAttribute("loggedInUser");
                if (user != null) {
                        Set<Integer> wishlistIds = wishlistService.getWishlistByUserId(user.getId())
                                        .stream()
                                        .map(ProductVariant::getVariantID)
                                        .collect(Collectors.toSet());
                        model.addAttribute("wishlistIds", wishlistIds);
                }

                // Thêm tham số để xác định có hiển thị form đánh giá không
                if (showReview != null && showReview) {
                    model.addAttribute("showReviewForm", true);
                    
                    // Kiểm tra xem người dùng đã đăng nhập chưa
                    if (user == null) {
                        // Nếu chưa đăng nhập, lưu URL hiện tại để chuyển hướng sau khi đăng nhập
                        String redirectUrl = "/detail/" + variantId + "?review=true";
                        session.setAttribute("redirectAfterLogin", redirectUrl);
                    }
                } else {
                    model.addAttribute("showReviewForm", false);
                }

                model.addAttribute("v", variant);
                model.addAttribute("sameStorageVariants", sameStorageVariants);
                model.addAttribute("uniqueVariantsByStorage", uniqueVariantsByStorage.values());
                model.addAttribute("specifications", specifications);
                model.addAttribute("products", otherProducts);

                return "views/user/products-detail";
        }

}