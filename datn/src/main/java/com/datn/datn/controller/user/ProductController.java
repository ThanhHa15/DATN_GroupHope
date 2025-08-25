package com.datn.datn.controller.user;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.datn.datn.model.Category;
import com.datn.datn.model.Member;
import com.datn.datn.model.Product;
import com.datn.datn.model.ProductVariant;
import com.datn.datn.repository.CategoryRepository;
import com.datn.datn.service.CategoryService;
import com.datn.datn.service.ProductService;
import com.datn.datn.service.ProductVariantService;
import com.datn.datn.service.WishlistService;

import jakarta.servlet.http.HttpSession;

@RequestMapping("/products")
@Controller
public class ProductController {
    private final ProductVariantService productVariantService;
    private final CategoryService categoryService;
    // private final ProductService productService; // Thêm service này nếu cần
    private final WishlistService wishlistService;
    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryRepository categoryRepository;

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
            @RequestParam(required = false) List<String> price,
            @RequestParam(required = false) List<String> type,
            @RequestParam(required = false) List<String> storage,
            @RequestParam(required = false) Boolean discounted,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String keyword,
            Model model, HttpSession session,
            @ModelAttribute("message") String message) {

        List<ProductVariant> variants;

        if (Boolean.TRUE.equals(discounted)) { // Kiểm tra nếu có lọc theo giảm giá
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

        // Thêm danh sách danh mục vào model
        List<Category> allCategories = categoryService.getAll();
        model.addAttribute("allCategories", allCategories);

        // 🧮 Lọc theo loại
        if (type != null && !type.isEmpty()) {
            System.out.println("Đang lọc theo danh mục: " + type);

            variants = variants.stream()
                    .filter(v -> {
                        if (v.getProduct() == null || v.getProduct().getCategory() == null) {
                            System.out.println(
                                    "Sản phẩm " + (v.getProduct() != null ? v.getProduct().getProductName() : "null")
                                            + " không có category");
                            return false;
                        }

                        String dbCategoryName = v.getProduct().getCategory().getName().trim();
                        boolean match = type.stream()
                                .anyMatch(t -> t.trim().equalsIgnoreCase(dbCategoryName));

                        System.out.println("Kiểm tra: " + v.getProduct().getProductName()
                                + " - Category: " + dbCategoryName
                                + " - Match: " + match);

                        return match;
                    })
                    .collect(Collectors.toList());
        }

        // 💾 Lọc theo dung lượng
        if (storage != null && !storage.isEmpty()) {
            variants = variants.stream()
                    .filter(v -> v.getStorage() != null && storage.contains(v.getStorage()))
                    .collect(Collectors.toList());
        }

        // 💰 Lọc theo giá
        if (price != null && !price.isEmpty()) {
            variants = variants.stream().filter(v -> {
                BigDecimal priceValue = v.getDiscountedPrice() != null ? v.getDiscountedPrice() : BigDecimal.ZERO;
                return price.stream().anyMatch(p -> {
                    switch (p) {
                        case "Dưới 5 triệu":
                            return priceValue.compareTo(BigDecimal.valueOf(5_000_000)) < 0;
                        case "5 - 10 triệu":
                            return priceValue.compareTo(BigDecimal.valueOf(5_000_000)) >= 0 &&
                                    priceValue.compareTo(BigDecimal.valueOf(10_000_000)) <= 0;
                        case "10 - 15 triệu":
                            return priceValue.compareTo(BigDecimal.valueOf(10_000_000)) > 0 &&
                                    priceValue.compareTo(BigDecimal.valueOf(15_000_000)) <= 0;
                        case "15 - 20 triệu":
                            return priceValue.compareTo(BigDecimal.valueOf(15_000_000)) > 0 &&
                                    priceValue.compareTo(BigDecimal.valueOf(20_000_000)) <= 0;
                        case "Trên 20 triệu":
                            return priceValue.compareTo(BigDecimal.valueOf(20_000_000)) > 0;
                    }
                    return false;
                });
            }).collect(Collectors.toList());
        }

        // 🔍 Lọc theo từ khóa tìm kiếm (tên sản phẩm)
        if (keyword != null && !keyword.trim().isEmpty()) {
            String lowerKeyword = keyword.trim().toLowerCase(); // thêm trim() ở đây
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

        model.addAttribute("selectedPrices", price);
        model.addAttribute("selectedTypes", type);
        model.addAttribute("selectedStorages", storage);
        model.addAttribute("products", pagedVariants);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("sort", sort);
        model.addAttribute("isDiscounted", discounted != null ? discounted : false);
        model.addAttribute("keyword", keyword);

        if (message != null && !message.isEmpty()) {
            model.addAttribute("message", message); // thêm vào model
        }
        List<Object[]> counts = productService.countProductsByCategory();
        Map<String, Long> categoryCounts = new HashMap<>();
        for (Object[] row : counts) {
            String categoryName = (String) row[0];
            Long count = (Long) row[1];
            categoryCounts.put(categoryName, count);
        }

        model.addAttribute("categoryCounts", categoryCounts);

        // Tạo URL gốc giữ các tham số lọc hiện tại
        StringBuilder baseUrl = new StringBuilder("/products?");
        if (sort != null)
            baseUrl.append("sort=").append(sort).append("&");
        if (discounted != null)
            baseUrl.append("discounted=").append(discounted).append("&");
        if (keyword != null && !keyword.isEmpty())
            baseUrl.append("keyword=").append(keyword).append("&");
        if (price != null)
            price.forEach(p -> baseUrl.append("price=").append(p).append("&"));
        if (type != null)
            type.forEach(t -> baseUrl.append("type=").append(t).append("&"));
        if (storage != null)
            storage.forEach(s -> baseUrl.append("storage=").append(s).append("&"));

        // Xóa ký tự & cuối nếu có
        if (baseUrl.charAt(baseUrl.length() - 1) == '&') {
            baseUrl.deleteCharAt(baseUrl.length() - 1);
        }

        model.addAttribute("baseUrl", baseUrl.toString());

        // Lấy danh sách category để lặp ở view
        model.addAttribute("categories", categoryRepository.findAll());
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
