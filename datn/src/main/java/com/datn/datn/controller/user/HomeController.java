package com.datn.datn.controller.user;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.datn.datn.model.Category;
import com.datn.datn.model.ProductVariant;
import com.datn.datn.model.RoleDetail;
import com.datn.datn.model.Users;
import com.datn.datn.model.Product;
import com.datn.datn.service.CategoryService;
import com.datn.datn.service.EmailService;
import com.datn.datn.service.ProductVariantService;
import com.datn.datn.service.UsersService;
import com.datn.datn.service.ProductService;

import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {
    private final ProductVariantService productVariantService;
    @Autowired
    private UsersService usersService;
    @Autowired
    private EmailService emailService;

    public HomeController(ProductVariantService productVariantService) {
        this.productVariantService = productVariantService;
    }

    @GetMapping("/")
    public String redirectToHome() {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String Home(Model model) {
        // Lấy danh sách sản phẩm mỗi (productid + storage) duy nhất
        List<ProductVariant> allVariants = productVariantService.findUniqueVariantsByProductAndStorage();

        // Giới hạn chỉ lấy 5 sản phẩm đầu tiên
        List<ProductVariant> limitedVariants = allVariants.stream()
                .limit(10)
                .collect(Collectors.toList());

        // Gán vào model để hiển thị ra trang chủ
        model.addAttribute("products", limitedVariants);

        return "views/user/trangchu";
    }

    @GetMapping("/login")
    public String login() {
        return "views/shared/login";
    }

    @PostMapping("/login")
    public String loginPost(RedirectAttributes redirectAttributes,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            Model model, HttpSession session) {
        try {
            if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("message", "Thông tin người dùng không được để trống!");
                return "redirect:/login";
            }
            Users users = usersService.login(email, password);

            if (users != null && Boolean.TRUE.equals(users.getActivated())) {
                session.setAttribute("currentUser", users);
                if (users.getRoleDetails() != null) {
                    for (RoleDetail roleDetail : users.getRoleDetails()) {
                        System.out.println(">> ROLE: " + roleDetail.getRole().getDescription());
                    }
                }
                System.out.println("isAdmin: " + isAdmin(users));
                System.out.println("isStaff: " + isStaff(users));
                System.out.println("isCustomer: " + isCustomer(users));

                if (isAdmin(users)) {
                    session.setAttribute("userAdmin", "Admin");
                    redirectAttributes.addFlashAttribute("loginSuccess", "Đăng nhập thành công (Admin)!");
                    return "redirect:/admin/employees"; // Admin đi đến trang quản lý nhân viên
                }

                if (isStaff(users)) {
                    session.setAttribute("userStaff", "Staff");
                    redirectAttributes.addFlashAttribute("loginSuccess", "Đăng nhập thành công (Staff)!");
                    return "redirect:/l"; // Staff đi đến trang chủ
                }
                if (isCustomer(users)) {
                    session.setAttribute("userCustomer", "Customer");
                    redirectAttributes.addFlashAttribute("loginSuccess", "Đăng nhập thành công (Customer)!");
                    return "redirect:/home"; // Customer đi đến trang chủ
                }

                redirectAttributes.addFlashAttribute("message", "Bạn không có quyền truy cập hệ thống!");
                return "redirect:/login";
            } else {
                redirectAttributes.addFlashAttribute("message",
                        "Email hoặc mật khẩu không đúng hoặc tài khoản chưa kích hoạt!");
                return "redirect:/login";
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("message", "Đã xảy ra lỗi hệ thống: " + e.getMessage());
            return "redirect:/login";
        }
    }

    private boolean isAdmin(Users users) {
        if (users.getRoleDetails() == null)
            return false;
        for (RoleDetail roleDetail : users.getRoleDetails()) {
            String roleName = roleDetail.getRole().getDescription();
            if (roleName != null && roleName.trim().equalsIgnoreCase("Admin")) {
                return true;
            }
        }
        return false;
    }

    private boolean isStaff(Users users) {
        if (users.getRoleDetails() == null)
            return false;
        for (RoleDetail roleDetail : users.getRoleDetails()) {
            String roleName = roleDetail.getRole().getDescription();
            if (roleName != null && roleName.trim().equalsIgnoreCase("Staff")) {
                return true;
            }
        }
        return false;
    }

    private boolean isCustomer(Users users) {
        if (users.getRoleDetails() == null)
            return false;
        for (RoleDetail roleDetail : users.getRoleDetails()) {
            String roleName = roleDetail.getRole().getDescription();
            if (roleName != null && roleName.trim().equalsIgnoreCase("Customer")) {
                return true;
            }
        }
        return false;
    }

    @PostMapping("/forgetPass")
    public String processForgetPass(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {
        Users user = usersService.findByEmail(email);
        if (user == null) {
            redirectAttributes.addFlashAttribute("message", "Email không tồn tại!");
            return "redirect:/forgetPass";
        }

        String newPassword = UUID.randomUUID().toString().substring(0, 8);
        user.setPassword(newPassword); // ⚠️ Bạn nên mã hóa mật khẩu nếu đang dùng mã hóa!
        usersService.save(user);

        // In ra console (tạm thời để debug)
        System.out.println("Mật khẩu mới: " + newPassword);

        // Gửi email
        emailService.sendNewPassword(email, newPassword);

        redirectAttributes.addFlashAttribute("message", "Mật khẩu mới đã được gửi đến email của bạn!");
        return "redirect:/forgetPass";
    }

    @GetMapping("/register")
    public String register(Model model, HttpSession session) {
        return "views/shared/register";
    }

    @GetMapping("/forgetPass")
    public String forgetPass(Model model, HttpSession session) {
        return "views/shared/forgetPass";
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

    @GetMapping("/guarantee")
    public String guarantee(Model model, HttpSession session) {
        return "views/user/baohanh";
    }

    @GetMapping("/formGuarantee")
    public String formGuarantee(Model model, HttpSession session) {
        return "views/user/tracuu-baohanh";
    }

    @GetMapping("/info")
    public String info(Model model, HttpSession session) {
        return "views/shared/info";
    }

    @GetMapping("/editInf")
    public String editInf(Model model, HttpSession session) {
        return "views/shared/editInf";
    }

    @GetMapping("/address")
    public String address(Model model, HttpSession session) {
        return "views/shared/address";
    }
}
