package com.datn.datn.controller.admin;

import com.datn.datn.model.Product;
import com.datn.datn.model.ProductSpecification;
import com.datn.datn.service.ProductService;
import com.datn.datn.service.impl.ProductSpecificationServiceImpl;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/specification")
public class ProductSpecificationController {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductSpecificationServiceImpl specificationService;

    @GetMapping
    public String showForm(Model model,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size, HttpSession session,
            RedirectAttributes redirectAttributes) { // 10 sản phẩm mỗi trang
        String role = (String) session.getAttribute("role");
        if (role == null || (!role.equals("ADMIN") && !role.equals("STAFF"))) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập trang này!");
            return "redirect:/access-denied"; // hoặc trả về 1 trang báo lỗi
        }
        // Get all products
        List<Product> allProducts = productService.getAll();

        // Lọc theo search nếu có
        List<Product> filteredProducts = allProducts.stream()
                .filter(p -> p.getProductName().toLowerCase().contains(search.toLowerCase()))
                .collect(Collectors.toList());

        // Pagination cho sản phẩm
        int totalProducts = filteredProducts.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalProducts / size));
        page = Math.min(Math.max(1, page), totalPages);
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, totalProducts);
        List<Product> pagedProducts = filteredProducts.subList(startIndex, endIndex);

        // Lấy tất cả thông số kỹ thuật cho các sản phẩm trong trang hiện tại
        List<Integer> pagedProductIds = pagedProducts.stream()
                .map(p -> p.getProductID().intValue())
                .collect(Collectors.toList());

        List<ProductSpecification> allSpecs = specificationService.findAll();
       List<ProductSpecification> relevantSpecs = allSpecs.stream()
        .filter(spec -> pagedProductIds.contains(spec.getProduct().getProductID()))
        .collect(Collectors.toList());

// Group specifications by productId
Map<Integer, List<ProductSpecification>> groupedSpecList = relevantSpecs.stream()
        .collect(Collectors.groupingBy(spec -> spec.getProduct().getProductID()));


        // Map id -> name cho các sản phẩm trong trang hiện tại
        Map<Integer, String> productIdToNameMap = pagedProducts.stream()
                .collect(Collectors.toMap(
                        p -> p.getProductID().intValue(),
                        Product::getProductName));

        // Add model attributes
        model.addAttribute("groupedSpecList", groupedSpecList);
        model.addAttribute("productIdToNameMap", productIdToNameMap);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("search", search);
        model.addAttribute("totalItems", totalProducts);

        return "formProductSpecification";
    }

    @PostMapping("/save")
    public String saveSpecifications(
            @RequestParam("productid") Integer productId,
            @RequestParam Map<String, String> allParams) {
        // Lặp qua tất cả các tham số
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            // Chỉ xử lý các tham số specs[...]
            if (entry.getKey().startsWith("specs[") && entry.getValue() != null && !entry.getValue().isEmpty()) {
                // Trích xuất tên thông số từ key
                String specKey = entry.getKey().substring(6, entry.getKey().length() - 1);

                // Tạo và lưu thông số kỹ thuật
                ProductSpecification spec = new ProductSpecification();
                Product product = productService.getById(productId);
                spec.setProduct(product);
                spec.setSpecKey(specKey);
                spec.setSpecValue(entry.getValue());

                specificationService.saveSpecification(spec);

                // Hiển thị thông tin (debug)
                System.out.println("Đã lưu: " + specKey + " = " + entry.getValue());
            }
        }

        return "redirect:/specification";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable("id") Integer id) {
        specificationService.deleteSpecification(id);
        return "redirect:/specification";
    }

    /* ===== cập nhật specValue ===== */
    @PostMapping("/update")
    public String update(@RequestParam Integer specId,
            @RequestParam String specValue) {
        specificationService.updateSpecificationValue(specId, specValue);
        return "redirect:/specification";
    }
}