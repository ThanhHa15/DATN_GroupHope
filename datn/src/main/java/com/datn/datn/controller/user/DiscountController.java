package com.datn.datn.controller.user;

import com.datn.datn.model.Product;
import com.datn.datn.model.ProductVariant;
import com.datn.datn.service.ProductService;
import com.datn.datn.service.ProductVariantService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin/discount")
public class DiscountController {

    private final ProductVariantService variantService;
    private final ProductService productService;

    public DiscountController(ProductVariantService variantService, ProductService productService) {
        this.variantService = variantService;
        this.productService = productService;
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

        // ❌ Ngày không hợp lệ
        if (start.isBefore(today) || end.isBefore(today)) {
            redirectAttributes.addFlashAttribute("error", "Ngày bắt đầu và kết thúc không được ở quá khứ.");
            return "redirect:/variants"; // quay lại form áp dụng
        }

        if (end.isBefore(start)) {
            redirectAttributes.addFlashAttribute("error", "Ngày kết thúc phải sau ngày bắt đầu.");
            return "redirect:/variants";
        }

        // ✅ Kiểm tra storage có tồn tại với productId không
        List<String> validStorages = variantService.findStoragesByProductId(productId);

        if (!validStorages.contains(storage)) {
            redirectAttributes.addFlashAttribute("error", "Dung lượng không hợp lệ cho sản phẩm đã chọn.");
            return "redirect:/variants";
        }

        // ✅ Tiến hành áp dụng giảm giá
        try {
            variantService.applyDiscountToStorage(productId, storage, discount, start, end);
            redirectAttributes.addFlashAttribute("success", "Áp dụng giảm giá thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Đã xảy ra lỗi khi áp dụng giảm giá.");
        }

        return "redirect:/variants"; // chuyển về trang danh sách biến thể sản phẩm
    }

}
