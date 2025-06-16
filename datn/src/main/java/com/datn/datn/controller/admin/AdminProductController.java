package com.datn.datn.controller.admin;

import com.datn.datn.model.Products;
import com.datn.datn.service.CategoriesService;
import com.datn.datn.service.ProductsService;
import com.datn.datn.model.Categories;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

    @Autowired
    private ProductsService productService;
    
    @Autowired
    private CategoriesService categoryService;
     
    public static String UPLOAD_DIRECTORY = System.getProperty("user.dir") + "/src/main/resources/static/images";
    @GetMapping("")
    public String home(Model model) {
        model.addAttribute("products", productService.findAll());
        model.addAttribute("categories", categoryService.findAlla());
        return "views/admin/admin-products";
    }

    @PostMapping("/add")
    public String addProduct(
            @ModelAttribute Products product,
            @RequestParam("imageFile") MultipartFile imageFile,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Xử lý upload ảnh
            if (!imageFile.isEmpty()) {
                Path uploadPath = Paths.get(UPLOAD_DIRECTORY);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                
                String fileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(imageFile.getInputStream(), filePath);
                product.setImage("/images/" + fileName);
            }
            
            productService.add(product);
            redirectAttributes.addFlashAttribute("success", "Thêm sản phẩm thành công");
            redirectAttributes.addFlashAttribute("refresh", true); // Thêm dòng này
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        
        return "redirect:/admin/products";
    }

    @PostMapping("/update")
    public String updateProduct(
            @ModelAttribute Products product,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Lấy thông tin sản phẩm hiện tại
            Products existingProduct = productService.findById(product.getId());
            
            // Chỉ cập nhật ảnh nếu có file mới được chọn
            if (imageFile != null && !imageFile.isEmpty()) {
                String fileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
                Path fileNameAndPath = Paths.get(UPLOAD_DIRECTORY, fileName);
                Files.write(fileNameAndPath, imageFile.getBytes());
                product.setImage("/images/" + fileName);
            } else {
                // Giữ nguyên ảnh cũ nếu không có ảnh mới
                product.setImage(existingProduct.getImage());
            }
            
            productService.update(product);
            redirectAttributes.addFlashAttribute("success", "Sản phẩm đã được cập nhật thành công");
            redirectAttributes.addFlashAttribute("refresh", true);
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Error uploading image: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating product: " + e.getMessage());
        }
        
        return "redirect:/admin/products";
    }

    @PostMapping("/delete")
    public String deleteProduct(@RequestParam Integer id, RedirectAttributes redirectAttributes) {
        try {
            // Lấy thông tin sản phẩm trước khi xóa để xóa file ảnh nếu cần
            Products product = productService.findById(id);
            
            if (product != null) {
                // Xóa file ảnh nếu có
                if (product.getImage() != null && !product.getImage().isEmpty()) {
                    String imagePath = UPLOAD_DIRECTORY + "/" + product.getImage().replace("/images/", "");
                    Path fileToDelete = Paths.get(imagePath);
                    if (Files.exists(fileToDelete)) {
                        Files.delete(fileToDelete);
                    }
                }
                
                productService.delete(id);
                redirectAttributes.addFlashAttribute("success", "Sản phẩm đã được xóa thành công");
                redirectAttributes.addFlashAttribute("refresh", true);
            } else {
                redirectAttributes.addFlashAttribute("error", "Product not found");
            }
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting product image: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting product: " + e.getMessage());
        }
        
        return "redirect:/admin/products";
    }
}