package com.datn.datn.controller.user;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.datn.datn.model.Category;
import com.datn.datn.model.ProductVariant;
import com.datn.datn.model.Product;
import com.datn.datn.service.CategoryService;
import com.datn.datn.service.ProductVariantService;
import com.datn.datn.service.ProductService;

import jakarta.servlet.http.HttpSession;

@RequestMapping("/products")
@Controller
public class ProductController {
    private final ProductVariantService productVariantService;
    private final CategoryService categoryService;
    private final ProductService productService; // Thêm service này nếu cần

    public ProductController(ProductService productService,
            ProductVariantService productVariantService,
            CategoryService categoryService) {
        this.productService = productService;
        this.productVariantService = productVariantService;
        this.categoryService = categoryService;
    }

    @GetMapping
    public String listProducts(
            @RequestParam(required = false) Boolean discounted,
            @RequestParam(required = false) String sort,
            Model model) {

        List<ProductVariant> variants;

        if (Boolean.TRUE.equals(discounted)) {
            variants = productVariantService.findDiscountedVariants()
                    .stream()
                    .collect(Collectors.collectingAndThen(
                            Collectors.toMap(
                                    v -> v.getProduct().getProductID() + "-" +
                                            (v.getStorage() != null ? v.getStorage() : "no-storage"),
                                    v -> v,
                                    (v1, v2) -> v1),
                            map -> map.values().stream().collect(Collectors.toList())));
        } else {
            variants = productVariantService.findUniqueVariantsByProductAndStorage();
        }

        // Sắp xếp theo yêu cầu
        if ("name_asc".equalsIgnoreCase(sort)) {
            variants.sort(Comparator.comparing(v -> v.getProduct().getProductName(), String.CASE_INSENSITIVE_ORDER));
        } else if ("name_desc".equalsIgnoreCase(sort)) {
            variants.sort(Comparator.comparing(
                    (ProductVariant v) -> v.getProduct().getProductName(), String.CASE_INSENSITIVE_ORDER).reversed());
        } else if ("price_asc".equalsIgnoreCase(sort)) {
            variants.sort(Comparator.comparing(v -> {
                BigDecimal discountedPrice = v.getDiscountedPrice();
                return discountedPrice != null ? discountedPrice : BigDecimal.valueOf(0);
            }));
        } else if ("price_desc".equalsIgnoreCase(sort)) {
            variants.sort(Comparator.comparing((ProductVariant v) -> {
                BigDecimal discountedPrice = v.getDiscountedPrice();
                return discountedPrice != null ? discountedPrice : BigDecimal.valueOf(0);
            }).reversed());
        }

        // 👉 Giới hạn tối đa 15 sản phẩm
        variants = variants.stream()
                .limit(15)
                .collect(Collectors.toList());

        model.addAttribute("sort", sort); // truyền sort về Thymeleaf
        model.addAttribute("products", variants);
        model.addAttribute("isEmpty", variants.isEmpty());
        model.addAttribute("isDiscounted", discounted != null ? discounted : false);

        return "views/user/products";
    }

}
