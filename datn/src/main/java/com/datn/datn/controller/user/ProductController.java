package com.datn.datn.controller.user;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.datn.datn.model.Category;
import com.datn.datn.model.Member;
import com.datn.datn.model.ProductVariant;
import com.datn.datn.model.Product;
import com.datn.datn.service.CategoryService;
import com.datn.datn.service.ProductVariantService;
import com.datn.datn.service.WishlistService;
import com.datn.datn.service.ProductService;

import jakarta.servlet.http.HttpSession;

@RequestMapping("/products")
@Controller
public class ProductController {
    private final ProductVariantService productVariantService;
    private final CategoryService categoryService;
    private final ProductService productService; // Thêm service này nếu cần
    private final WishlistService wishlistService;

    public ProductController(ProductService productService,
            ProductVariantService productVariantService,
            CategoryService categoryService,
            WishlistService wishlistService) {
        this.productService = productService;
        this.productVariantService = productVariantService;
        this.categoryService = categoryService;
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public String listProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) Boolean discounted,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String keyword,
            Model model, HttpSession session,
            @ModelAttribute("message") String message) {

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

        // 🔍 Lọc theo từ khóa tìm kiếm (tên sản phẩm)
        if (keyword != null && !keyword.trim().isEmpty()) {
            String lowerKeyword = keyword.toLowerCase();
            variants = variants.stream()
                    .filter(v -> v.getProduct().getProductName().toLowerCase().contains(lowerKeyword))
                    .collect(Collectors.toList());
        }

        // 🧠 Thêm danh sách wishlistIds nếu đã đăng nhập
        Member user = (Member) session.getAttribute("loggedInUser");
        if (user != null) {
            Set<Integer> wishlistIds = wishlistService.getWishlistByUserId(user.getId())
                    .stream()
                    .map(ProductVariant::getVariantID)
                    .collect(Collectors.toSet());
            model.addAttribute("wishlistIds", wishlistIds);
        }

        // Sắp xếp
        if ("name_asc".equalsIgnoreCase(sort)) {
            variants.sort(Comparator.comparing(v -> v.getProduct().getProductName(), String.CASE_INSENSITIVE_ORDER));
        } else if ("name_desc".equalsIgnoreCase(sort)) {
            variants.sort(Comparator
                    .comparing((ProductVariant v) -> v.getProduct().getProductName(), String.CASE_INSENSITIVE_ORDER)
                    .reversed());
        } else if ("price_asc".equalsIgnoreCase(sort)) {
            variants.sort(Comparator.comparing(v -> {
                BigDecimal discountedPrice = v.getDiscountedPrice();
                return discountedPrice != null ? discountedPrice : BigDecimal.ZERO;
            }));
        } else if ("price_desc".equalsIgnoreCase(sort)) {
            variants.sort(Comparator.comparing((ProductVariant v) -> {
                BigDecimal discountedPrice = v.getDiscountedPrice();
                return discountedPrice != null ? discountedPrice : BigDecimal.ZERO;
            }).reversed());
        }

        // Phân trang
        int totalItems = variants.size();
        int totalPages = (int) Math.ceil((double) totalItems / size);
        int fromIndex = Math.min((page - 1) * size, totalItems);
        int toIndex = Math.min(fromIndex + size, totalItems);
        List<ProductVariant> pagedVariants = variants.subList(fromIndex, toIndex);

        model.addAttribute("products", pagedVariants);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("sort", sort);
        model.addAttribute("isDiscounted", discounted != null ? discounted : false);
        model.addAttribute("keyword", keyword);

        if (message != null && !message.isEmpty()) {
            model.addAttribute("message", message); // thêm vào model
        }
        return "views/user/products";
    }

    @GetMapping("/search-suggestions")
    @ResponseBody
    public List<String> getSearchSuggestions(@RequestParam("query") String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of(); // Không gợi ý nếu rỗng
        }

        String lowerQuery = query.toLowerCase();

        // Lấy tất cả productName chứa từ khóa (giới hạn 10 gợi ý)
        return productService.getAll().stream()
                .map(Product::getProductName)
                .filter(name -> name != null && name.toLowerCase().contains(lowerQuery))
                .distinct()
                .limit(10)
                .collect(Collectors.toList());
    }

}
