package com.datn.datn.controller.admin;

import com.datn.datn.model.Category;
import com.datn.datn.model.Product;
import com.datn.datn.model.ProductVariant;
import com.datn.datn.service.CategoryService;
import com.datn.datn.service.ProductService;
import com.datn.datn.service.ProductVariantService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/variants")
public class ProductVariantController {

    private final ProductVariantService variantService;
    private final ProductService productService;
    private final CategoryService categoryService;

    public ProductVariantController(ProductVariantService variantService,
            ProductService productService,
            CategoryService categoryService) {
        this.variantService = variantService;
        this.productService = productService;
        this.categoryService = categoryService;
    }

    // ========== HIỂN THỊ DANH SÁCH & FORM ==========
    @GetMapping
    public String showVariantForm(
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        String role = (String) session.getAttribute("role");
        if (role == null || (!role.equals("ADMIN") && !role.equals("STAFF"))) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập trang này!");
            return "redirect:/access-denied";
        }

        Page<ProductVariant> variantPage = variantService.getAll(
                PageRequest.of(page, size, Sort.by("product.productName").descending()));

        List<Product> products = loadProductsWithStorages();

        model.addAttribute("variant", new ProductVariant());
        model.addAttribute("variants", variantPage.getContent());
        model.addAttribute("products", products);
        model.addAttribute("categories", categoryService.getAll());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", variantPage.getTotalPages());

        return "formVariant";
    }

    // ========== TÌM KIẾM ==========
    @GetMapping("/search")
    public String searchVariants(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String status,
            Model model) {

        Page<ProductVariant> variantPage = variantService.searchVariantsByNameWithFilters(
                keyword, categoryId, status, PageRequest.of(page, size));
        List<Product> products = loadProductsWithStorages();

        model.addAttribute("variant", new ProductVariant());
        model.addAttribute("variants", variantPage.getContent());
        model.addAttribute("products", products);
        model.addAttribute("categories", categoryService.getAll());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("status", status);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", variantPage.getTotalPages());

        if (variantPage.isEmpty()) {
            model.addAttribute("notFoundMessage", "Không tìm thấy kết quả cho từ khóa: \"" + keyword + "\"");
        }

        return "formVariant";
    }

    // ========== LỌC KẾT HỢP (DANH MỤC + TRẠNG THÁI) ==========
    @GetMapping("/filter")
    public String filterVariants(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("product.productName").descending());
        Page<ProductVariant> variantPage;

        // Sử dụng phương thức lọc kết hợp
        variantPage = variantService.filterVariantsWithMultipleFilters(categoryId, status, keyword, pageable);

        List<Product> products = loadProductsWithStorages();

        model.addAttribute("variant", new ProductVariant());
        model.addAttribute("variants", variantPage.getContent());
        model.addAttribute("products", products);
        model.addAttribute("categories", categoryService.getAll());
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("status", status);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", variantPage.getTotalPages());

        // Tạo thông báo phù hợp
        if (variantPage.isEmpty()) {
            StringBuilder message = new StringBuilder("Không tìm thấy sản phẩm");
            if (keyword != null && !keyword.trim().isEmpty()) {
                message.append(" với từ khóa: \"").append(keyword).append("\"");
            }
            if (categoryId != null && categoryId > 0) {
                Category category = categoryService.getById(categoryId);
                if (category != null) {
                    message.append(" trong danh mục: \"").append(category.getName()).append("\"");
                }
            }
            if (status != null && !status.isEmpty()) {
                String statusText = status.equals("con") ? "còn hàng" : "hết hàng";
                message.append(" với trạng thái: \"").append(statusText).append("\"");
            }
            model.addAttribute("notFoundMessage", message.toString());
        }

        return "formVariant";
    }

    // ========== CÁC PHƯƠNG THỨC CŨ VẪN GIỮ NGUYÊN ==========

    // Giữ nguyên phương thức filterVariantsByCategory cho tương thích ngược
    @GetMapping("/filter-variants")
    public String filterVariantsByCategory(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            Model model) {

        return filterVariants(categoryId, null, null, page, size, model);
    }

    // Giữ nguyên phương thức filterVariantsByStatus cho tương thích ngược
    @GetMapping("/filter-status")
    public String filterVariantsByStatus(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            Model model) {

        return filterVariants(null, status, null, page, size, model);
    }

    @PostMapping("/update")
    public String updateVariant(
            @ModelAttribute("variant") ProductVariant variant,
            @RequestParam("imageFile") MultipartFile imageFile,
            RedirectAttributes redirectAttributes) {

        try {
            if (!imageFile.isEmpty()) {
                String fileName = imageFile.getOriginalFilename();
                String uploadDir = new File("src/main/resources/static/images").getAbsolutePath();
                File dest = new File(uploadDir, fileName);
                imageFile.transferTo(dest);
                variant.setImagesno2(fileName);
            } else if (variant.getVariantID() != null) {
                ProductVariant existing = variantService.getById(variant.getVariantID());
                if (existing != null) {
                    variant.setImagesno2(existing.getImagesno2());
                }
            }

            variantService.save(variant);
            redirectAttributes.addFlashAttribute("success", "Cập nhật phiên bản thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi cập nhật: " + e.getMessage());
        }

        return "redirect:/variants";
    }

    @GetMapping("/delete/{id}")
    public String deleteVariant(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            variantService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Xóa thành công!");
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", "Không thể xóa vì sản phẩm này đã nằm trong đơn hàng!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra, vui lòng thử lại!");
        }
        return "redirect:/variants";
    }

    @GetMapping("/edit/{id}")
    public String editVariant(@PathVariable Integer id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            Model model) {
        ProductVariant variant = variantService.getById(id);
        if (variant == null) {
            return "redirect:/variants";
        }

        if (variant.getProduct() != null) {
            variant.setProductId(variant.getProduct().getProductID());
        }

        Page<ProductVariant> variantPage = variantService.getAll(PageRequest.of(page, size));
        List<Product> products = loadProductsWithStorages();

        model.addAttribute("variant", variant);
        model.addAttribute("products", products);
        model.addAttribute("variants", variantPage.getContent());
        model.addAttribute("categories", categoryService.getAll());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", variantPage.getTotalPages());

        return "formVariant";
    }

    private List<Product> loadProductsWithStorages() {
        List<Product> products = productService.getAll();
        for (Product p : products) {
            p.setStorages(variantService.findStoragesByProductId(p.getProductID()));
        }
        return products;
    }
}