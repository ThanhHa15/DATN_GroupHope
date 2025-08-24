package com.datn.datn.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.datn.datn.model.Category;
import com.datn.datn.model.Member;
import com.datn.datn.service.CategoryService;
import com.datn.datn.service.MembersService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;
    @Autowired
    private MembersService memberService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public String showCategoryForm(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        String role = (String) session.getAttribute("role");
        if (role == null || !role.equals("ADMIN")) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập trang này!");
            return "redirect:/access-denied";
        }

        model.addAttribute("category", new Category());
        model.addAttribute("categories", categoryService.getAll());

        return "formCategory";
    }

    @PostMapping("/add")
    public String saveCategory(@ModelAttribute Category category) {
        categoryService.save(category);
        return "redirect:/categories";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        categoryService.delete(id);
        return "redirect:/categories";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Integer id, Model model) {
        Category category = categoryService.getById(id);
        model.addAttribute("category", category);
        model.addAttribute("categories", categoryService.getAll());
        return "formCategory";
    }
}
