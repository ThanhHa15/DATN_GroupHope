package com.datn.datn.controller.admin;

import com.datn.datn.dto.ProductVariantCreateDTO;
import com.datn.datn.dto.VariantSpecificationDTO;
import com.datn.datn.model.Product;
import com.datn.datn.model.ProductVariant;
import com.datn.datn.service.*;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/variants")
public class ProductVariantController {

    private final ProductVariantService variantService;
    private final ProductService productService;

    public ProductVariantController(ProductVariantService variantService,
            ProductService productService) {
        this.variantService = variantService;
        this.productService = productService;
    }

    @GetMapping
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
        return "formVariant"; // tên file view Thymeleaf của bạn
    }

    @PostMapping("/add")
    public String addOrUpdateVariant(
            @ModelAttribute("variant") ProductVariant variant,
            @RequestParam("imageFile") MultipartFile imageFile,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("products", productService.getAll());
            return "formVariant";
        }

        // Xử lý ảnh
        if (!imageFile.isEmpty()) {
            try {
                String fileName = imageFile.getOriginalFilename();
                String uploadDir = new File("src/main/resources/static/images").getAbsolutePath();
                File dest = new File(uploadDir, fileName);
                imageFile.transferTo(dest);
                variant.setImagesno2(fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (variant.getVariantID() != null) {
            ProductVariant existing = variantService.getById(variant.getVariantID());
            if (existing != null) {
                variant.setImagesno2(existing.getImagesno2());
            }
        }

        variantService.save(variant);
        return "redirect:/variants";
    }

    @GetMapping("/delete/{id}")
    public String deleteVariant(@PathVariable Integer id) {
        variantService.delete(id);
        return "redirect:/variants";
    }

    @GetMapping("/edit/{id}")
    public String editVariant(@PathVariable Integer id, Model model) {
        // Lấy variant cần sửa
        ProductVariant variant = variantService.getById(id);

        // Gán productId để dùng cho form binding (vì dùng *{productId})
        variant.setProductId(variant.getProduct().getProductID());

        // Lấy danh sách sản phẩm
        List<Product> products = productService.getAll();

        // Với mỗi sản phẩm, gán thêm danh sách các dung lượng (storages)
        for (Product product : products) {
            List<String> storages = variantService.findStoragesByProductId(product.getProductID());
            product.setStorages(storages);
        }

        // Đẩy dữ liệu ra view
        model.addAttribute("variant", variant);
        model.addAttribute("products", products);
        model.addAttribute("variants", variantService.getAll());

        return "formVariant";
    }

}
