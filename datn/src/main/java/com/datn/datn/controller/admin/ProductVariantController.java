package com.datn.datn.controller.admin;

import com.datn.datn.model.Category;
import com.datn.datn.model.Product;
import com.datn.datn.model.ProductVariant;
import com.datn.datn.service.CategoryService;
import com.datn.datn.service.ProductService;
import com.datn.datn.service.ProductVariantService;

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
            @RequestParam(defaultValue = "8") int size) {

        // sắp xếp A-Z theo tên sản phẩm (giả sử field là 'name')
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
            Model model) {

        Page<ProductVariant> variantPage = variantService.searchVariantsByName(keyword, PageRequest.of(page, size));
        List<Product> products = loadProductsWithStorages();

        model.addAttribute("variant", new ProductVariant());
        model.addAttribute("variants", variantPage.getContent());
        model.addAttribute("products", products);
        model.addAttribute("categories", categoryService.getAll());
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", variantPage.getTotalPages());

        // ✅ Thêm thông báo khi không có kết quả
        if (variantPage.isEmpty()) {
            model.addAttribute("notFoundMessage", "Không tìm thấy kết quả cho từ khóa: \"" + keyword + "\"");
        }

        return "formVariant";
    }

    // ========== LỌC THEO DANH MỤC ==========
    @GetMapping("/filter-variants")
    public String filterVariantsByCategory(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ProductVariant> variantPage;

        if (categoryId == null || categoryId == 0) {
            variantPage = variantService.getAll(pageable); // lấy tất cả
        } else {
            variantPage = variantService.filterByCategory(categoryId, pageable); // lọc
        }

        List<Product> products = loadProductsWithStorages();

        model.addAttribute("variant", new ProductVariant());

        model.addAttribute("variants", variantPage.getContent());
        model.addAttribute("products", products);
        model.addAttribute("categories", categoryService.getAll());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", variantPage.getTotalPages());
        model.addAttribute("selectedCategoryId", categoryId);

        // ✅ nếu không có sản phẩm
        if (variantPage.isEmpty()) {
            model.addAttribute("noProductMessage", "Không có sản phẩm trong danh mục này");
        }

        return "formVariant";
    }

    @PostMapping("/update")
    public String updateVariant(
            @ModelAttribute("variant") ProductVariant variant,
            @RequestParam("imageFile") MultipartFile imageFile,
            RedirectAttributes redirectAttributes) {

        try {
            // Xử lý ảnh
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

    // ========== XÓA ==========
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

    // ========== SỬA ==========
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

    // ========== LỌC THEO TRẠNG THÁI ==========
    @GetMapping("/filter-status")
    public String filterVariantsByStatus(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            Model model) {

        Page<ProductVariant> variantPage = variantService.filterByStatus(status, PageRequest.of(page, size));
        List<Product> products = loadProductsWithStorages();

        model.addAttribute("variant", new ProductVariant());
        model.addAttribute("variants", variantPage.getContent());
        model.addAttribute("products", products);
        model.addAttribute("categories", categoryService.getAll());
        model.addAttribute("status", status);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", variantPage.getTotalPages());

        return "formVariant";
    }

    // ========== HÀM CHUNG: LOAD PRODUCTS KÈM STORAGES ==========
    private List<Product> loadProductsWithStorages() {
        List<Product> products = productService.getAll();
        for (Product p : products) {
            p.setStorages(variantService.findStoragesByProductId(p.getProductID()));
        }
        return products;
    }
}