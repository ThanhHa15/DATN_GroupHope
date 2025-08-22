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
import java.util.List;

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
            Model model) {
        List<Product> products = productService.getAll();

        for (Product p : products) {
            List<String> storages = variantService.findStoragesByProductId(p.getProductID());
            p.setStorages(storages);
        }

        // Lấy danh sách categories
        List<Category> categories = categoryService.getAll();

        // Phân trang cho variants
        List<ProductVariant> allVariants = variantService.getAll();
        int totalItems = allVariants.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / size));
        int fromIndex = Math.min((page - 1) * size, totalItems);
        int toIndex = Math.min(fromIndex + size, totalItems);
        List<ProductVariant> variants = allVariants.subList(fromIndex, toIndex);

        model.addAttribute("variant", new ProductVariant());
        model.addAttribute("variants", variants);
        model.addAttribute("products", products);
        model.addAttribute("categories", categories);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("size", size);
        model.addAttribute("totalItems", totalItems);
        return "formDiscount";
    }

    @PostMapping("/apply")
    public String applyDiscount(
            @RequestParam("productId") Integer productId,
            @RequestParam("storage") String storage,
            @RequestParam("discount") float discount,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            RedirectAttributes redirectAttributes) {

        LocalDate today = LocalDate.now();

        if (start.isBefore(today) || end.isBefore(today)) {
            redirectAttributes.addFlashAttribute("error", "Ngày bắt đầu và kết thúc không được ở quá khứ.");
            return "redirect:/variants/discounts";
        }

        if (end.isBefore(start)) {
            redirectAttributes.addFlashAttribute("error", "Ngày kết thúc phải sau ngày bắt đầu.");
            return "redirect:/variants/discounts";
        }

        if (discount < 0 || discount > 100) {
            redirectAttributes.addFlashAttribute("error", "Giá trị giảm giá phải từ 0 đến 100.");
            return "redirect:/variants/discounts";
        }

        List<String> validStorages = variantService.findStoragesByProductId(productId);

        if (!validStorages.contains(storage)) {
            redirectAttributes.addFlashAttribute("error", "Dung lượng không hợp lệ cho sản phẩm đã chọn.");
            return "redirect:/variants/discounts";
        }

        try {
            variantService.applyDiscountToStorage(productId, storage, discount, start, end);
            redirectAttributes.addFlashAttribute("success", "Áp dụng giảm giá thành công!");
        } catch (Exception e) {
            logger.error("Lỗi khi áp dụng giảm giá: ", e);
            redirectAttributes.addFlashAttribute("error", "Đã xảy ra lỗi khi áp dụng giảm giá.");
        }

        return "redirect:/variants/discounts";
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
        return "redirect:/variants/discounts";
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
            return "redirect:/variants/discounts";
        }

        // Kiểm tra ngày bắt đầu không được lớn hơn ngày kết thúc
        if (start.isAfter(end)) {
            redirectAttributes.addFlashAttribute("error", "Ngày bắt đầu không được lớn hơn ngày kết thúc.");
            return "redirect:/variants/discounts";
        }

        if (discount < 0 || discount > 100) {
            redirectAttributes.addFlashAttribute("error", "Giá trị giảm giá phải từ 0 đến 100.");
            return "redirect:/variants/discounts";
        }

        try {
            variantService.updateDiscount(variantId, discount, start, end);
            redirectAttributes.addFlashAttribute("success", "Cập nhật giảm giá thành công!");
        } catch (Exception e) {
            logger.error("Lỗi khi cập nhật giảm giá: ", e);
            redirectAttributes.addFlashAttribute("error", "Cập nhật giảm giá thất bại: " + e.getMessage());
        }
        return "redirect:/variants/discounts";
    }
    


}
