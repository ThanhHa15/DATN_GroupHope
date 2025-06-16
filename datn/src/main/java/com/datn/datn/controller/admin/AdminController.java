package com.datn.datn.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.datn.datn.model.Categories;
import com.datn.datn.repository.CategoriesRepository;
import com.datn.datn.service.CategoriesService;

@Controller
@RequestMapping("/admin")
public class AdminController {
    @GetMapping("")
    public String home(Model model) {
        return "views/admin/admin";
    }
   
}
