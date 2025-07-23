package com.datn.datn.controller.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.datn.datn.model.Cart;
import com.datn.datn.model.Category;
import com.datn.datn.model.Member;
import com.datn.datn.model.ProductVariant;
import com.datn.datn.repository.CartRepository;
import com.datn.datn.repository.MemberRepository;
import com.datn.datn.repository.ProductVariantRepository;
import com.datn.datn.repository.WishlistRepository;
import com.datn.datn.model.Product;
import com.datn.datn.service.CategoryService;
import com.datn.datn.service.EmailService;
import com.datn.datn.service.ProductVariantService;
import com.datn.datn.service.WishlistService;
import com.datn.datn.service.MembersService;
import com.datn.datn.service.ProductService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class HomeAdminController {
    @GetMapping("dashboard")
    public String dashboard(Model model, HttpServletRequest request) {
        return "lll";
    }
    

}
