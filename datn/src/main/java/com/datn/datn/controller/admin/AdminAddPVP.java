package com.datn.datn.controller.admin;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import com.datn.datn.model.Product;
import com.datn.datn.model.ProductSpecification;
import com.datn.datn.model.ProductVariant;
import com.datn.datn.service.CategoryService;
import com.datn.datn.service.ProductService;
import com.datn.datn.service.ProductSpecificationService;
import com.datn.datn.service.ProductVariantService;

@Controller
@RequestMapping("/admin-addPVP")
public class AdminAddPVP {
    private final ProductService productService;
    private final CategoryService categoryService;
    private final ProductVariantService variantService;
    private final ProductSpecificationService specificationService;

    public AdminAddPVP(ProductService productService, CategoryService categoryService,
            ProductVariantService variantService, ProductSpecificationService specificationService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.variantService = variantService;
        this.specificationService = specificationService;
    }

    @GetMapping
    public String PVP(Model model) {
        List<Product> products = productService.getAll();
        List<String> productNames = products.stream()
                .map(p -> p.getProductName().trim().toLowerCase())
                .collect(Collectors.toList());
        model.addAttribute("products", products);
        model.addAttribute("productNames", productNames);
        for (Product p : products) {
            List<String> storages = variantService.findStoragesByProductId(p.getProductID());
            p.setStorages(storages);
        }
        model.addAttribute("variant", new ProductVariant());
        model.addAttribute("categories", categoryService.getAll());
        model.addAttribute("products", products);
        model.addAttribute("product", new Product());
        return "formAddPVP";
    }

    @PostMapping("/saveAll")
    @Transactional
    public String saveAll(
            @ModelAttribute("product") Product product,
            @ModelAttribute("variant") ProductVariant variant,
            @RequestParam("productImageFile") MultipartFile productImageFile,
            @RequestParam("variantImageFile") MultipartFile variantImageFile,
            @RequestParam Map<String, String> allParams,
            Model model) {

        try {
            // 1. Lưu Product trước
            // BỎ TOÀN BỘ ĐOẠN XỬ LÝ productImageFile
            product.setImageUrl(null); // Đảm bảo luôn null
            Product savedProduct = productService.save(product);
            if (savedProduct == null || savedProduct.getProductID() == null) {
                model.addAttribute("error", "Không lưu được Product!");
                return PVP(model);
            }

            // 2. Lưu ProductVariant
            variant.setProduct(savedProduct);
            if (!variantImageFile.isEmpty()) {
                String fileName = variantImageFile.getOriginalFilename();
                String uploadDir = new File("src/main/resources/static/images").getAbsolutePath();
                File dest = new File(uploadDir, fileName);
                variantImageFile.transferTo(dest);
                variant.setImagesno2(fileName);
            }
            String priceInput = allParams.get("priceInput");
            if (priceInput != null && !priceInput.isEmpty()) {
                String cleanedPrice = priceInput.replaceAll("[.,\\s]", "");
                try {
                    double price = Double.parseDouble(cleanedPrice);
                    variant.setPrice(price);
                } catch (NumberFormatException e) {
                }
            }
            if (product.getManufactureDate() == null || product.getManufactureDate().isAfter(LocalDate.now())) {
                model.addAttribute("error", "Ngày sản xuất không hợp lệ");
                return PVP(model);
            }
            variantService.save(variant);

            // 3. Lưu ProductSpecification
            for (Map.Entry<String, String> entry : allParams.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("specs[") && key.endsWith("]")) {
                    String value = entry.getValue();
                    String specKey = key.substring(6, key.length() - 1);
                    if (value != null && !value.trim().isEmpty()) {
                        ProductSpecification spec = new ProductSpecification();
                        spec.setProduct(savedProduct); // LUÔN dùng savedProduct đã có productID
                        spec.setSpecKey(specKey);
                        spec.setSpecValue(value.trim());
                        specificationService.saveSpecification(spec);
                    }
                }
            }

            return "redirect:/admin-addPVP?success=true";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Lỗi khi lưu dữ liệu: " + e.getMessage());
            return "formAddPVP";
        }
    }

}
