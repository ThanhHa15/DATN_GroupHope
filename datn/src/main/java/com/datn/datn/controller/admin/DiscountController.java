// Code này nhìn chung đã ổn, đã tuân thủ các nguyên tắc cơ bản của Spring MVC:
// - Sử dụng annotation controller, mapping rõ ràng
// - Validate dữ liệu đầu vào (ngày, storage hợp lệ)
// - Xử lý lỗi và trả thông báo qua RedirectAttributes
// - Phân tách service cho nghiệp vụ

// Một số góp ý nhỏ để code tốt hơn:
// 1. Có thể log lỗi chi tiết hơn trong các catch (dùng logger).
// 2. Nên validate discount (ví dụ: discount >= 0 && discount <= 100).
// 3. Có thể tách logic validate ra service hoặc sử dụng @Valid cho form.
// 4. Nếu hệ thống lớn, có thể dùng DTO thay vì truyền trực tiếp các tham số.

// Ví dụ bổ sung log lỗi và validate discount:

package com.datn.datn.controller.admin;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.datn.datn.model.Category;
import com.datn.datn.model.Product;
import com.datn.datn.model.ProductVariant;
import com.datn.datn.service.CategoryService;
import com.datn.datn.service.ProductService;
import com.datn.datn.service.ProductVariantService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/discount")
public class DiscountController {
    // @Autowired
    // private CategoryService categoryService;

    private final ProductVariantService variantService;
    private final ProductService productService;
    private final CategoryService categoryService;
    private static final Logger logger = LoggerFactory.getLogger(DiscountController.class);

    public DiscountController(ProductVariantService variantService,
            ProductService productService,
            CategoryService categoryService) {
        this.variantService = variantService;
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping
    public String showVariantForm(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort, // new param
            Model model, HttpSession session,
            RedirectAttributes redirectAttributes) {
        String role = (String) session.getAttribute("role");
        if (role == null || (!role.equals("ADMIN") && !role.equals("STAFF"))) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập trang này!");
            return "redirect:/access-denied"; // hoặc trả về 1 trang báo lỗi
        }

        List<ProductVariant> allVariants = variantService.getAll();

        // filter theo category
        if (name != null && !name.isEmpty()) {
            allVariants = allVariants.stream()
                    .filter(v -> v.getProduct().getCategory().getName().equalsIgnoreCase(name))
                    .collect(Collectors.toList());
        }

        // filter theo keyword
        if (keyword != null && !keyword.isEmpty()) {
            allVariants = allVariants.stream()
                    .filter(v -> v.getProduct().getProductName().toLowerCase().contains(keyword.toLowerCase()))
                    .collect(Collectors.toList());
        }

        // sort theo tên sản phẩm
        if ("desc".equalsIgnoreCase(sort)) {
            allVariants = allVariants.stream()
                    .sorted(Comparator.comparing((ProductVariant v) -> v.getProduct().getProductName()).reversed())
                    .collect(Collectors.toList());
        } else { // mặc định asc
            allVariants = allVariants.stream()
                    .sorted(Comparator.comparing(v -> v.getProduct().getProductName()))
                    .collect(Collectors.toList());
        }

        List<Product> products = productService.getAll();
        for (Product p : products) {
            List<String> storages = variantService.findStoragesByProductId(p.getProductID());
            p.setStorages(storages);
        }
        model.addAttribute("products", products);

        // phân trang
        int totalItems = allVariants.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / size));
        int fromIndex = Math.min((page - 1) * size, totalItems);
        int toIndex = Math.min(fromIndex + size, totalItems);
        List<ProductVariant> variants = allVariants.subList(fromIndex, toIndex);

        model.addAttribute("variants", variants);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("size", size);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("categories", categoryService.getAll());
        model.addAttribute("keyword", keyword);
        model.addAttribute("name", name);
        model.addAttribute("sort", sort);
        // THÊM DÒNG NÀY để truyền danh sách sản phẩm cho dropdown
        model.addAttribute("products", productService.getAll());
        return "formDiscount";
    }

    @PostMapping("/apply")
    public String applyDiscount(
            @RequestParam("productId") Integer productId,
            @RequestParam("storage") String storage,
            @RequestParam("discount") float discount,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            RedirectAttributes redirectAttributes, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (role == null || !role.equals("ADMIN")) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập trang này!");
            return "redirect:/access-denied";
        }

        LocalDate today = LocalDate.now();

        if (start.isBefore(today) || end.isBefore(today)) {
            redirectAttributes.addFlashAttribute("error", "Ngày bắt đầu và kết thúc không được ở quá khứ.");
            return "redirect:/discount";
        }

        if (end.isBefore(start)) {
            redirectAttributes.addFlashAttribute("error", "Ngày kết thúc phải sau ngày bắt đầu.");
            return "redirect:/discount";
        }

        if (discount < 0 || discount > 100) {
            redirectAttributes.addFlashAttribute("error", "Giá trị giảm giá phải từ 0 đến 100.");
            return "redirect:/discount";
        }

        List<String> validStorages = variantService.findStoragesByProductId(productId);

        if (!validStorages.contains(storage)) {
            redirectAttributes.addFlashAttribute("error", "Dung lượng không hợp lệ cho sản phẩm đã chọn.");
            return "redirect:/discount";
        }

        try {
            variantService.applyDiscountToStorage(productId, storage, discount, start, end);
            redirectAttributes.addFlashAttribute("success", "Áp dụng giảm giá thành công!");
        } catch (Exception e) {
            logger.error("Lỗi khi áp dụng giảm giá: ", e);
            redirectAttributes.addFlashAttribute("error", "Đã xảy ra lỗi khi áp dụng giảm giá.");
        }

        return "redirect:/discount";
    }

    @PostMapping("/remove/{variantId}")
    public String removeDiscount(@PathVariable Integer variantId, RedirectAttributes redirectAttributes) {
        try {
            variantService.removeDiscount(variantId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa giảm giá thành công!");
        } catch (Exception e) {
            logger.error("Lỗi khi xóa giảm giá: ", e);
            redirectAttributes.addFlashAttribute("error", "Xóa giảm giá thất bại: " + e.getMessage());
        }
        return "redirect:/discount";
    }

    @PostMapping("/update")
    public String updateDiscount(
            @RequestParam("variantId") Integer variantId,
            @RequestParam("discount") float discount,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            RedirectAttributes redirectAttributes) {

        LocalDate today = LocalDate.now();

        // Kiểm tra ngày kết thúc không được ở trong quá khứ
        if (end.isBefore(today)) {
            redirectAttributes.addFlashAttribute("error", "Ngày kết thúc không được ở trong quá khứ.");
            return "redirect:/discount";
        }

        // Kiểm tra ngày bắt đầu không được lớn hơn ngày kết thúc
        if (start.isAfter(end)) {
            redirectAttributes.addFlashAttribute("error", "Ngày bắt đầu không được lớn hơn ngày kết thúc.");
            return "redirect:/discount";
        }

        if (discount < 0 || discount > 100) {
            redirectAttributes.addFlashAttribute("error", "Giá trị giảm giá phải từ 0 đến 100.");
            return "redirect:/discount";
        }

        try {
            variantService.updateDiscount(variantId, discount, start, end);
            redirectAttributes.addFlashAttribute("success", "Cập nhật giảm giá thành công!");
        } catch (Exception e) {
            logger.error("Lỗi khi cập nhật giảm giá: ", e);
            redirectAttributes.addFlashAttribute("error", "Cập nhật giảm giá thất bại: " + e.getMessage());
        }
        return "redirect:/discount";
    }

}
