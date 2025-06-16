package com.datn.datn.controller.admin;

import com.datn.datn.model.Categories;
import com.datn.datn.service.CategoriesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
public class AdminCateController {

    @Autowired
    private CategoriesService categoriesService;

    @GetMapping("")
    public String home(Model model) {
        model.addAttribute("categories", categoriesService.findAlla());
        return "views/admin/admin-categories";
    }

    @PostMapping("/add")
    public String addCategory(@RequestParam String name, RedirectAttributes redirectAttributes) {
        if (name == null || name.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Category name cannot be empty");
            return "redirect:/admin/categories";
        }
        
        if (categoriesService.isNameExists(name)) {
            redirectAttributes.addFlashAttribute("error", "Category name already exists");
            return "redirect:/admin/categories";
        }
        
        Categories category = new Categories();
        category.setName(name);
        categoriesService.add(category);
        redirectAttributes.addFlashAttribute("success", "Thêm danh mục thành công");
        return "redirect:/admin/categories";
    }

    @PostMapping("/update")
    public String updateCategory(@RequestParam Long id, 
                               @RequestParam String name,
                               RedirectAttributes redirectAttributes) {
        Categories category = categoriesService.findById(id);
        if (category == null) {
            redirectAttributes.addFlashAttribute("error", "Category not found");
            return "redirect:/admin/categories";
        }
        
        if (name == null || name.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Category name cannot be empty");
            return "redirect:/admin/categories";
        }
        
        category.setName(name);
        categoriesService.update(category);
        redirectAttributes.addFlashAttribute("success", "Danh mục đã được cập nhật thành công");
        return "redirect:/admin/categories";
    }

    @PostMapping("/delete")
    public String deleteCategory(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        try {
            categoriesService.deleteWithProducts(id);
            redirectAttributes.addFlashAttribute("success", "Danh mục đã được xóa thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting category: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }
}