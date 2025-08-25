package com.datn.datn.controller.admin;

import com.datn.datn.model.Category;
import com.datn.datn.model.Product;
import com.datn.datn.repository.WishlistRepository;
import com.datn.datn.service.CategoryService;
import com.datn.datn.service.ProductService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/admin-products")
public class AdminProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    public AdminProductController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping
    public String showProductForm(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {

        // Sắp xếp Z -> A theo tên sản phẩm
        Page<Product> productPage = productService.getAll(
                PageRequest.of(page, size, Sort.by("productName").descending()));

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("categories", categoryService.getAll());
        model.addAttribute("product", null);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("pageSize", size);

        // Base URL mặc định
        model.addAttribute("baseUrl", "/admin-products?");

        return "formProduct";
    }

    @PostMapping("/add")
    public String addProduct(@ModelAttribute Product product,
            @RequestParam("imageFile") MultipartFile imageFile, RedirectAttributes redirectAttributes) {
        if (!imageFile.isEmpty()) {
            try {
                String fileName = imageFile.getOriginalFilename();
                String uploadDir = new File("src/main/resources/static/images").getAbsolutePath();
                File dest = new File(uploadDir, fileName);
                imageFile.transferTo(dest);
                product.setImageUrl(fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (product.getProductID() != null) {
                Product existing = productService.getById(product.getProductID());
                if (existing != null) {
                    product.setImageUrl(existing.getImageUrl());
                    existing.setStatus(product.isStatus());
                }
            }
        }

        if (product.getProductID() != null) {
            Product existing = productService.getById(product.getProductID());
            if (existing != null) {
                existing.setProductName(product.getProductName());
                existing.setDescription(product.getDescription());
                existing.setCategory(product.getCategory());
                existing.setStatus(product.isStatus());

                // Xử lý ngày sản xuất
                if (product.getManufactureDate() != null) {
                    existing.setManufactureDate(product.getManufactureDate());
                }

                // Xử lý ảnh
                if (product.getImageUrl() != null) {
                    existing.setImageUrl(product.getImageUrl());
                }

                productService.update(existing);
            }
            redirectAttributes.addFlashAttribute("message", "Cập nhật sản phẩm thành công!");
        } else {
            if (product.getManufactureDate() == null) {
                product.setManufactureDate(java.time.LocalDate.now());
            }
            productService.save(product);
            redirectAttributes.addFlashAttribute("message", "Thêm sản phẩm thành công!");
        }

        return "redirect:/admin-products";
    }

    @GetMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Integer id) {
        productService.delete(id);
        return "redirect:/admin-products";
    }

    @GetMapping("/edit/{id}")
    public String editProduct(@PathVariable Integer id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            Model model) {
        Product product = productService.getById(id);
        System.out.println("Manufacture Date: " + product.getManufactureDate());
        List<Category> categories = categoryService.getAll();

        Page<Product> productPage = productService.getAll(PageRequest.of(page, size));

        model.addAttribute("product", product);
        model.addAttribute("categories", categories);
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("pageSize", size);
        System.out.println("Ngay sx " + product.getManufactureDate());

        return "formProduct";
    }

    @GetMapping("/search")
    public String searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            Model model) {

        var productPage = productService.searchProductsByName(keyword, PageRequest.of(page, size));

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("categories", categoryService.getAll());
        model.addAttribute("product", new Product());
        model.addAttribute("keyword", keyword);

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("pageSize", size);

        model.addAttribute("baseUrl", "/admin-products/search?keyword=" + keyword + "&");

        return "formProduct";
    }

    @GetMapping("/filter")
    public String filterProductsByCategory(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            Model model) {

        Page<Product> productPage;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("productName").descending());

        if (categoryId == null || categoryId == 0) {
            productPage = productService.getAll(pageRequest);
        } else {
            productPage = productService.getByCategoryId(categoryId, pageRequest);
        }

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("categories", categoryService.getAll());
        model.addAttribute("product", new Product());
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("pageSize", size);

        model.addAttribute("baseUrl",
                "/admin-products/filter?categoryId=" + (categoryId == null ? 0 : categoryId) + "&");

        return "formProduct";
    }

    @GetMapping("/toggle-status/{id}")
    public String toggleStatus(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            productService.toggleStatus(id);
            redirectAttributes.addFlashAttribute("message", "Cập nhật trạng thái thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }
        return "redirect:/admin-products";
    }

}
