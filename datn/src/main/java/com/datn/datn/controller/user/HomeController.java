package com.datn.datn.controller.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
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

import com.datn.datn.model.Category;
import com.datn.datn.model.Member;
import com.datn.datn.model.ProductVariant;
import com.datn.datn.repository.MemberRepository;
import com.datn.datn.model.Product;
import com.datn.datn.service.CategoryService;
import com.datn.datn.service.EmailService;
import com.datn.datn.service.ProductVariantService;
import com.datn.datn.service.MembersService;
import com.datn.datn.service.ProductService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class HomeController {
    private final ProductVariantService productVariantService;
    @Autowired
    private MembersService memberService;
    @Autowired
    private EmailService emailService;
    @Autowired
    private MemberRepository memberRepository;

    public HomeController(ProductVariantService productVariantService) {
        this.productVariantService = productVariantService;
    }

    @GetMapping("/")
    public String redirectToHome() {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String home(Model model, HttpSession session,
            @ModelAttribute("loginSuccess") String loginSuccess) {

        // Gán thông báo đăng nhập (nếu có) từ flash attribute
        if (loginSuccess != null && !loginSuccess.isEmpty()) {
            model.addAttribute("loginSuccess", loginSuccess);
        }

        // Gán thông tin người dùng đã đăng nhập vào model
        Object loggedInUser = session.getAttribute("loggedInUser");
        model.addAttribute("loggedInUser", loggedInUser);

        // Lấy danh sách sản phẩm duy nhất theo productId + storage
        List<ProductVariant> allVariants = productVariantService.findUniqueVariantsByProductAndStorage();

        // Giới hạn lấy 10 sản phẩm
        List<ProductVariant> limitedVariants = allVariants.stream()
                .limit(10)
                .collect(Collectors.toList());

        model.addAttribute("products", limitedVariants);

        return "views/user/trangchu";
    }

    @GetMapping("/login")
    public String login(Model model, HttpSession session) {
        return "views/shared/login";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam String input,
            @RequestParam String password,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model) {

        Optional<Member> optional = memberRepository.loginByPhoneOrEmail(input, password);

        if (optional.isPresent()) {
            Member member = optional.get();
            session.setAttribute("loggedInUser", member);

            switch (member.getRole()) {
                case "ADMIN":
                    return "redirect:/admin/employees";
                case "STAFF":
                    return "redirect:/admin-products";
                case "CUSTOMER":
                    // Đặt cờ hiển thị thông báo vào Session
                    session.setAttribute("showLoginSuccess", true);
                    return "redirect:/"; // Redirect về trang chủ (không trả view trực tiếp)
                default:
                    model.addAttribute("error", "Không xác định được vai trò");
                    return "views/shared/login";
            }
        } else {
            model.addAttribute("error", "Sai email/số điện thoại hoặc mật khẩu");
            return "views/shared/login";
        }
    }

    @PostMapping("/clear-login-message")
    @ResponseBody
    public String clearLoginMessage(HttpSession session) {
        session.removeAttribute("showLoginSuccess"); // Xóa cờ hiển thị
        return "OK"; // Không cần trả về gì đặc biệt
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate(); // Hủy toàn bộ session
        redirectAttributes.addFlashAttribute("logoutSuccess", "Bạn đã đăng xuất thành công!");
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String register(Model model, HttpSession session) {
        model.addAttribute("member", new Member());
        return "views/shared/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("member") Member member,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (!member.getPassword().equals(member.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.confirmPassword", "Mật khẩu xác nhận không khớp");
        }

        if (memberRepository.existsByEmail(member.getEmail())) {
            result.rejectValue("email", "error.email", "Email đã tồn tại");
        }

        if (memberRepository.existsByPhone(member.getPhone())) {
            result.rejectValue("phone", "error.phone", "Số điện thoại đã tồn tại");
        }

        if (result.hasErrors()) {
            model.addAttribute("member", member);
            return "views/shared/register";
        }

        // ✅ Gán mặc định quyền khách hàng
        member.setRole("CUSTOMER");

        memberRepository.save(member);
        redirectAttributes.addFlashAttribute("success", "Đăng ký thành công!");
        return "redirect:/login";
    }

    @GetMapping("/forgetPass")
    public String forgetPass(Model model, HttpSession session) {
        return "views/shared/forgetPass";
    }

    @PostMapping("/forgetPass")
    public String processForgetPass(
            @RequestParam("email") String email,
            RedirectAttributes redirectAttributes) {

        Member member = memberService.findByEmail(email);
        if (member == null) {
            redirectAttributes.addFlashAttribute("message", "Email không tồn tại!");
            return "redirect:/forgetPass";
        }

        String newPassword = UUID.randomUUID().toString().substring(0, 8);
        member.setPassword(newPassword); // ⚠ Bạn nên mã hóa nếu đang dùng BCrypt!

        memberService.save(member); // Đã sửa lại tên đúng

        // In ra console (chỉ để test)
        System.out.println("Mật khẩu mới của " + email + " là: " + newPassword);

        // Gửi email (nếu có service email)
        emailService.sendNewPassword(email, newPassword);

        redirectAttributes.addFlashAttribute("message", "Mật khẩu mới đã được gửi đến email của bạn!");
        return "redirect:/forgetPass";
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
