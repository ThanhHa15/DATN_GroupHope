package com.datn.datn.controller.admin;

import com.datn.datn.model.Category;
import com.datn.datn.model.Product;
import com.datn.datn.service.CategoryService;
import com.datn.datn.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public String showProductForm(Model model) {
        List<Product> products = productService.getAll();
        List<Category> categories = categoryService.getAll();

        model.addAttribute("products", products);
        model.addAttribute("categories", categories);
        model.addAttribute("product", new Product());

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
            // Giữ lại ảnh cũ nếu đang trong chế độ cập nhật
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
    public String editProduct(@PathVariable Integer id, Model model) {
        Product product = productService.getById(id);
        List<Category> categories = categoryService.getAll();

        model.addAttribute("product", product);
        model.addAttribute("categories", categories);
        model.addAttribute("products", productService.getAll());

        return "formProduct";
    }
}
