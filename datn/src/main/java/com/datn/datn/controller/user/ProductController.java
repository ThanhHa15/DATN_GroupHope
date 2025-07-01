package com.datn.datn.controller.user;

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
            Model model) {

        List<ProductVariant> variants;

        if (Boolean.TRUE.equals(discounted)) {
            // Lọc các variant đang giảm giá, sau đó loại trùng theo productId + storage (có
            // thể null)
            variants = productVariantService.findDiscountedVariants()
                    .stream()
                    .collect(Collectors.collectingAndThen(
                            Collectors.toMap(
                                    v -> v.getProduct().getProductID() + "-"
                                            + (v.getStorage() != null ? v.getStorage() : "no-storage"), // nếu null thì
                                                                                                        // thay thế
                                    v -> v,
                                    (v1, v2) -> v1 // nếu trùng thì giữ bản đầu tiên
                            ),
                            map -> map.values().stream().collect(Collectors.toList())));
        } else {
            // Lấy tất cả các sản phẩm duy nhất theo productId + storage
            variants = productVariantService.findUniqueVariantsByProductAndStorage();
        }

        model.addAttribute("products", variants);
        model.addAttribute("isEmpty", variants.isEmpty());
        model.addAttribute("isDiscounted", discounted != null ? discounted : false);

        return "views/user/products";
    }

}
