package com.datn.datn.controller.admin;

import com.datn.datn.model.Category;
import com.datn.datn.model.Member;
import com.datn.datn.model.Order;
import com.datn.datn.model.Product;
import com.datn.datn.service.CategoryService;
import com.datn.datn.service.OrderService;
import com.datn.datn.service.ProductService;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin-products")
public class AdminProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    @Autowired // Thêm annotation này
    private OrderService orderService;

    public AdminProductController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping
    public String showProductForm(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        Page<Product> productPage = productService.getAll(PageRequest.of(page, size));

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("categories", categoryService.getAll());
        model.addAttribute("product", new Product());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("pageSize", size);

        // Base URL mặc định
        model.addAttribute("baseUrl", "/admin-products?");

        return "formProduct";
    }

    @PostMapping("/add")
    public String addProduct(@ModelAttribute Product product,
            @RequestParam("imageFile") MultipartFile imageFile) {
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
                }
            }
        }

        if (product.getProductID() != null) {
            productService.update(product);
        } else {
            productService.save(product);
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
        List<Category> categories = categoryService.getAll();

        Page<Product> productPage = productService.getAll(PageRequest.of(page, size));

        model.addAttribute("product", product);
        model.addAttribute("categories", categories);
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("pageSize", size);

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
        if (categoryId == null || categoryId == 0) {
            productPage = productService.getAll(PageRequest.of(page, size));
        } else {
            productPage = productService.getByCategoryId(categoryId, PageRequest.of(page, size));
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

    // @GetMapping("/filter-status")
    // public String filterProductsByStatus(
    // @RequestParam(required = false) boolean status,
    // @RequestParam(defaultValue = "0") int page,
    // @RequestParam(defaultValue = "8") int size,
    // Model model) {

    // Page<Product> productPage;

    // if (status == null || status.isEmpty()) {
    // productPage = productService.getAll(PageRequest.of(page, size));
    // } else {
    // productPage = productService.getByStatus(status, PageRequest.of(page, size));
    // }

    // model.addAttribute("products", productPage.getContent());
    // model.addAttribute("categories", categoryService.getAll());
    // model.addAttribute("product", new Product());
    // model.addAttribute("selectedStatus", status);
    // model.addAttribute("currentPage", page);
    // model.addAttribute("totalPages", productPage.getTotalPages());
    // model.addAttribute("pageSize", size);

    // return "formProduct";
    // }

}
