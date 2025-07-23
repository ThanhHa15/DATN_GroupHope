package com.datn.datn.controller.admin;

import com.datn.datn.model.Product;
import com.datn.datn.model.ProductVariant;
import com.datn.datn.service.ProductService;
import com.datn.datn.service.ProductVariantService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
// @RequestMapping("/discount")
public class DiscountController {

    private final ProductVariantService variantService;
    private final ProductService productService;

    public DiscountController(ProductVariantService variantService, ProductService productService) {
        this.variantService = variantService;
        this.productService = productService;
    }

    @GetMapping("/discount")
    public String showVariantForm(Model model) {
        List<Product> products = productService.getAll();

        // Gán storages cho từng product
        for (Product p : products) {
            List<String> storages = variantService.findStoragesByProductId(p.getProductID());
            p.setStorages(storages); // Bạn cần có getter/setter & @Transient trong Product
        }

        model.addAttribute("variant", new ProductVariant());
        model.addAttribute("variants", variantService.getAll());
        model.addAttribute("products", products); // thêm bản đã gán storages
        return "formDiscount"; // tên file view Thymeleaf của bạn
    }

    @PostMapping("/discount/apply")
    public String applyDiscount(
            @RequestParam("productId") Integer productId,
            @RequestParam("storage") String storage,
            @RequestParam("discount") float discount,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            RedirectAttributes redirectAttributes) {

        LocalDate today = LocalDate.now();

        // ❌ Validate ngày
        if (start.isBefore(today) || end.isBefore(today)) {
            redirectAttributes.addFlashAttribute("error", "Ngày bắt đầu và kết thúc không được ở quá khứ.");
            return "redirect:/discount";
        }

        if (end.isBefore(start)) {
            redirectAttributes.addFlashAttribute("error", "Ngày kết thúc phải sau ngày bắt đầu.");
            return "redirect:/discount";
        }

        // ✅ Kiểm tra tồn tại storage
        List<String> validStorages = variantService.findStoragesByProductId(productId);

        if (!validStorages.contains(storage)) {
            redirectAttributes.addFlashAttribute("error", "Dung lượng không hợp lệ cho sản phẩm đã chọn.");
            return "redirect:/discount";
        }

        try {
            variantService.applyDiscountToStorage(productId, storage, discount, start, end);
            redirectAttributes.addFlashAttribute("success", "Áp dụng giảm giá thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Đã xảy ra lỗi khi áp dụng giảm giá.");
        }

        return "redirect:/discount";
    }
}
