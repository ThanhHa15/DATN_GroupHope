// package com.datn.datn.controller.admin;

// import java.io.File;
// import java.io.IOException;
// import java.time.LocalDate;
// import java.util.List;
// import java.util.Map;

// import org.springframework.stereotype.Controller;
// import org.springframework.ui.Model;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.ModelAttribute;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestParam;
// import org.springframework.web.multipart.MultipartFile;

// import com.datn.datn.model.Product;
// import com.datn.datn.model.ProductSpecification;
// import com.datn.datn.model.ProductVariant;
// import com.datn.datn.service.CategoryService;
// import com.datn.datn.service.ProductService;
// import com.datn.datn.service.ProductSpecificationService;
// import com.datn.datn.service.ProductVariantService;

// @Controller
// @RequestMapping("/admin-addPVP")
// public class AdminAddPVP {
//     private final ProductService productService;
//     private final CategoryService categoryService;
//     private final ProductVariantService variantService;
//     private final ProductSpecificationService specificationService;

//     public AdminAddPVP(ProductService productService, CategoryService categoryService,
//             ProductVariantService variantService, ProductSpecificationService specificationService) {
//         this.productService = productService;
//         this.categoryService = categoryService;
//         this.variantService = variantService;
//         this.specificationService = specificationService;
//     }

//     @GetMapping
//     public String PVP(Model model) {
//         List<Product> products = productService.getAll();

//         for (Product p : products) {
//             List<String> storages = variantService.findStoragesByProductId(p.getProductID());
//             p.setStorages(storages);
//         }
//         model.addAttribute("variant", new ProductVariant());
//         model.addAttribute("categories", categoryService.getAll());
//         model.addAttribute("products", products);
//         model.addAttribute("product", new Product());
//         return "formAddPVP";
//     }

//     @PostMapping("/saveAll")
//     public String saveAll(
//             @ModelAttribute("product") Product product,
//             @ModelAttribute("variant") ProductVariant variant,
//             @RequestParam("productImageFile") MultipartFile productImageFile,
//             @RequestParam("variantImageFile") MultipartFile variantImageFile,
//             @RequestParam Map<String, String> allParams,
//             Model model) {

//         try {
//             // 1. Lưu Product
//             if (!productImageFile.isEmpty()) {
//                 String fileName = productImageFile.getOriginalFilename();
//                 String uploadDir = new File("src/main/resources/static/images").getAbsolutePath();
//                 File dest = new File(uploadDir, fileName);
//                 productImageFile.transferTo(dest);
//                 product.setImageUrl(fileName);
//             }

//             Product savedProduct = productService.save(product);

//             // 2. Lưu ProductVariant
//             variant.setProduct(savedProduct); // Gán product đã lưu cho variant

//             if (!variantImageFile.isEmpty()) {
//                 String fileName = variantImageFile.getOriginalFilename();
//                 String uploadDir = new File("src/main/resources/static/images").getAbsolutePath();
//                 File dest = new File(uploadDir, fileName);
//                 variantImageFile.transferTo(dest);
//                 variant.setImagesno2(fileName);
//             }

//             // Xử lý giá từ format VNĐ sang double
//             String priceInput = allParams.get("priceInput");
//             if (priceInput != null && !priceInput.isEmpty()) {
//                 String cleanedPrice = priceInput.replaceAll("[.,\\s]", "");
//                 try {
//                     double price = Double.parseDouble(cleanedPrice);
//                     variant.setPrice(price);
//                 } catch (NumberFormatException e) {
//                     // Xử lý lỗi nếu cần
//                 }
//             }

//             // Kiểm tra ngày sản xuất
//             if (variant.getManufactureDate() == null || variant.getManufactureDate().isAfter(LocalDate.now())) {
//                 model.addAttribute("error", "Ngày sản xuất không hợp lệ");
//                 return PVP(model);
//             }

//             ProductVariant savedVariant = variantService.save(variant);

//             // 3. Lưu ProductSpecification
//             Integer productId = savedProduct.getProductID();

//             for (Map.Entry<String, String> entry : allParams.entrySet()) {
//                 if (entry.getKey().startsWith("specs[") && entry.getValue() != null && !entry.getValue().isEmpty()) {
//                     String specKey = entry.getKey().substring(6, entry.getKey().length() - 1);

//                     ProductSpecification spec = new ProductSpecification();
//                     spec.setProductid(productId.intValue());
//                     spec.setSpecKey(specKey);
//                     spec.setSpecValue(entry.getValue());

//                     specificationService.saveSpecification(spec);
//                 }
//             }

//             return "redirect:/admin-addPVP";

//         } catch (IOException e) {
//             model.addAttribute("error", "Lỗi khi upload file: " + e.getMessage());
//             return PVP(model);
//         } catch (Exception e) {
//             model.addAttribute("error", "Lỗi hệ thống: " + e.getMessage());
//             return PVP(model);
//         }
//     }

// }
