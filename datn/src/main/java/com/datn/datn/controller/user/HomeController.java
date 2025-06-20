package com.datn.datn.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.datn.datn.model.Categories;
import com.datn.datn.model.Products;
import com.datn.datn.service.CategoriesService;
import com.datn.datn.service.ProductsService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/")
public class HomeController {
    @Autowired
    private ProductsService productsService;
 @Autowired
    private CategoriesService categoriesService;


    @GetMapping("/")
    public String Home(
    @RequestParam(value = "category", required = false) Long categoryId,
            Model model) {
        
        // Truyền danh sách tất cả các danh mục vào view
        model.addAttribute("categories", categoriesService.findAlla());
        
        // Xử lý lọc sản phẩm theo danh mục
        if (categoryId != null) {
            Categories selectedCategory = categoriesService.findById(categoryId);
            model.addAttribute("selectedCategory", selectedCategory);
            model.addAttribute("products", productsService.findByCategory(selectedCategory));
        } else {
            // Nếu không chọn danh mục nào thì hiển thị tất cả sản phẩm
            model.addAttribute("products", productsService.findAll());
        }
        
        return "views/user/trangchu";
}

    @GetMapping("/login")
    public String login(Model model, HttpSession session) {
        return "views/shared/login";
    }

    @GetMapping("/register")
    public String register(Model model, HttpSession session) {
        return "views/shared/register";
    }
     @GetMapping("/forgetPass")
    public String forgetPass(Model model, HttpSession session) {
        return "views/shared/forgetPass";
    }
    
    @GetMapping("/products")
    public String products(Model model, HttpSession session) {
        return "views/user/products";
    }

    @GetMapping("/detail")
    public String detail(Model model, HttpSession session) {
        return "views/user/products-detail";
    }

    @GetMapping("/introduction")
    public String introduction(Model model, HttpSession session) {
        return "views/user/introduction";
    }
    @GetMapping("/cart")
    public String cart(Model model, HttpSession session) {
        return "views/user/cart";
    }
    @GetMapping("/checkout")
    public String checkout(Model model, HttpSession session) {
        return "views/user/checkout";
    }
    @GetMapping("/order")
    public String order(Model model, HttpSession session) {
        return "views/user/order";
    }
    @GetMapping("/order-detail")
    public String orderDetail(Model model, HttpSession session) {
        return "views/user/orderDetail";
    }
}
