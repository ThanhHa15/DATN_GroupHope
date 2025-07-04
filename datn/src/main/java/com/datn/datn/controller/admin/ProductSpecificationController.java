package com.datn.datn.controller.admin;

import com.datn.datn.model.Product;
import com.datn.datn.model.ProductSpecification;
import com.datn.datn.service.ProductService;
import com.datn.datn.service.impl.ProductSpecificationServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/specification")
public class ProductSpecificationController {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductSpecificationServiceImpl specificationService;

    @GetMapping
    public String showForm(Model model) {
        List<Product> products = productService.getAll();
        model.addAttribute("products", products);
        return "formProductSpecification";
    }

   @PostMapping("/save")
    public String saveSpecifications(
            @RequestParam("productid") Long productId,
            @RequestParam Map<String, String> allParams) {
        // Lặp qua tất cả các tham số
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            // Chỉ xử lý các tham số specs[...]
            if (entry.getKey().startsWith("specs[") && entry.getValue() != null && !entry.getValue().isEmpty()) {
                // Trích xuất tên thông số từ key
                String specKey = entry.getKey().substring(6, entry.getKey().length() - 1);
                
                // Tạo và lưu thông số kỹ thuật
                ProductSpecification spec = new ProductSpecification();
                spec.setProductid(productId.intValue());
                spec.setSpecKey(specKey);
                spec.setSpecValue(entry.getValue());
                
                specificationService.saveSpecification(spec);
                
                // Hiển thị thông tin (debug)
                System.out.println("Đã lưu: " + specKey + " = " + entry.getValue());
            }
        }
        
        return "redirect:/specification";
    }
}