package com.datn.datn.controller.user;

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
import com.datn.datn.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
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

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private AuthService authService;

    public HomeController(ProductVariantService productVariantService) {
        this.productVariantService = productVariantService;
    }

    @GetMapping("/")
    public String redirectToHome() {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String home(Model model, HttpSession session,
            @ModelAttribute("loginSuccess") String loginSuccess,
            @ModelAttribute("message") String message,
            @ModelAttribute("type") String type) {

        if (message != null && !message.isEmpty()) {
            model.addAttribute("message", message);
            model.addAttribute("type", (type != null && !type.isEmpty()) ? type : "success");
        }

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

        // ✅ Thêm danh sách wishlist nếu người dùng đăng nhập
        if (loggedInUser instanceof Member user) {
            Set<Integer> wishlistIds = wishlistService.getWishlistByUserId(user.getId())
                    .stream()
                    .map(ProductVariant::getVariantID)
                    .collect(Collectors.toSet());
            model.addAttribute("wishlistIds", wishlistIds);
        }

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
            // 👉 Kiểm tra xem tài khoản có bị khóa không
            if (!member.isActive()) {
                model.addAttribute("error", "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.");
                return "views/shared/login";
            }
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
        return "redirect:/home";
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
            RedirectAttributes redirectAttributes) {

        // Kiểm tra lỗi đầu vào
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
            return "views/shared/register";
        }

        // Gán mặc định quyền và trạng thái
        member.setRole("CUSTOMER");
        member.setVerified(false);
        member.setActive(true);

        // Tạo và gán OTP
        String otp = authService.generateOtp();
        member.setOtp(otp);

        // Lưu thông tin member
        memberRepository.save(member);

        // Gửi mã OTP
        authService.sendOtp(member.getEmail(), otp);

        // Chuyển hướng đến trang nhập OTP
        redirectAttributes.addFlashAttribute("email", member.getEmail());
        return "redirect:/otp";
    }

    @GetMapping("/otp")
    public String showOtpPage(Model model) {
        return "views/shared/otp"; // Trang nhập OTP
    }

    @PostMapping("/verify-otp")
    public String handleOtp(
            @RequestParam String email,
            @RequestParam String otp,
            RedirectAttributes redirectAttributes) {

        try {
            boolean isVerified = authService.verifyOtp(email, otp);
            if (isVerified) {
                redirectAttributes.addFlashAttribute("success", "Xác thực thành công! Vui lòng đăng nhập.");
                return "redirect:/login"; // Chuyển hướng đến trang đăng nhập
            } else {
                redirectAttributes.addFlashAttribute("error", "Mã OTP không hợp lệ");
                return "redirect:/otp?email=" + email; // Quay lại trang OTP nếu sai
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi hệ thống khi xác thực OTP: " + e.getMessage());
            return "redirect:/otp?email=" + email;
        }
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
        Member user = (Member) session.getAttribute("loggedInUser");
        model.addAttribute("loggedInUser", user);

        List<Cart> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        if (user != null) {
            items = cartRepository.findByMember(user);

            for (Cart c : items) {
                ProductVariant v = c.getVariant();
                BigDecimal price = v.getDiscountedPrice() != null
                        ? v.getDiscountedPrice()
                        : BigDecimal.valueOf(v.getPrice());
                BigDecimal sub = price.multiply(BigDecimal.valueOf(c.getQuantity()));
                total = total.add(sub);
            }
        }

        model.addAttribute("cartItems", items);
        model.addAttribute("total", total);

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
    public String showInfo(Model model, HttpSession session) {
        Member member = (Member) session.getAttribute("loggedInUser"); // ✅ Sửa ở đây
        if (member == null) {
            return "redirect:/login";
        }
        // Tạo chuỗi ****** theo độ dài mật khẩu
        String maskedPassword = "*".repeat(member.getPassword().length());
        model.addAttribute("member", member);
        model.addAttribute("maskedPassword", maskedPassword); // Truyền chuỗi ẩn mật khẩu
        model.addAttribute("member", member);
        return "views/shared/info";
    }

    @GetMapping("/editInf")
    public String editInf(Model model, HttpSession session) {
        Member member = (Member) session.getAttribute("loggedInUser");

        if (member == null) {
            return "redirect:/login";
        }

        model.addAttribute("member", member);
        return "views/shared/editInf";
    }

    @PostMapping("/editInf")
    public String updateMemberInfo(HttpServletRequest request, HttpSession session, Model model) {
        Member currentUser = (Member) session.getAttribute("loggedInUser"); // ✅ Lấy đúng session

        if (currentUser == null) {
            return "redirect:/login"; // Phòng trường hợp bị null
        }

        // Lấy dữ liệu từ form
        String fullname = request.getParameter("fullname");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String birthdayStr = request.getParameter("birthday");

        String currentPassword = request.getParameter("current-password");
        String newPassword = request.getParameter("new-password");
        String confirmPassword = request.getParameter("confirm-password");

        // Cập nhật thông tin cơ bản
        currentUser.setFullname(fullname);
        currentUser.setEmail(email);
        currentUser.setPhone(phone);
        if (birthdayStr != null && !birthdayStr.isBlank()) {
            currentUser.setBirthday(LocalDate.parse(birthdayStr));
        }

        // Xử lý đổi mật khẩu nếu có
        if (currentPassword != null && !currentPassword.isBlank()) {
            if (!currentPassword.equals(currentUser.getPassword())) {
                model.addAttribute("error", "Mật khẩu hiện tại không đúng");
                model.addAttribute("member", currentUser);
                return "views/shared/editInf";
            }

            if (newPassword == null || newPassword.isBlank()) {
                model.addAttribute("error", "Vui lòng nhập mật khẩu mới");
                model.addAttribute("member", currentUser);
                return "views/shared/editInf";
            }

            if (!newPassword.equals(confirmPassword)) {
                model.addAttribute("error", "Mật khẩu xác nhận không khớp");
                model.addAttribute("member", currentUser);
                return "views/shared/editInf";
            }

            currentUser.setPassword(newPassword); // Nếu có BCrypt thì mã hóa ở đây
        }

        // Lưu thay đổi
        memberService.save(currentUser);
        session.setAttribute("loggedInUser", currentUser); // ✅ Cập nhật lại session

        model.addAttribute("success", "Cập nhật thông tin thành công");
        model.addAttribute("member", currentUser);
        return "views/shared/editInf"; // ✅ Load lại trang
    }

    @GetMapping("/address")
    public String address(Model model, HttpSession session) {
        return "views/shared/address";
    }

    @PostMapping("/api/cart/add")
    @ResponseBody
    public ResponseEntity<?> addToCart(
            @RequestParam Integer variantId,
            @RequestParam(defaultValue = "1") int quantity,
            HttpSession session) {

        Member loggedInUser = (Member) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập");
        }

        // ✅ Lấy variant trước để kiểm tra tồn kho
        Optional<ProductVariant> variantOpt = productVariantRepository.findById(variantId);
        if (variantOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Sản phẩm không tồn tại");
        }

        ProductVariant variant = variantOpt.get();

        // ✅ Kiểm tra tồn kho
        if (variant.getQuantityInStock() == 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Sản phẩm đã hết hàng");
        }

        if (variant.getQuantityInStock() < quantity) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Số lượng vượt quá số lượng tồn kho");
        }

        // ✅ Kiểm tra nếu sản phẩm đã có trong giỏ
        Optional<Cart> existingCart = cartRepository.findByMemberAndVariant(loggedInUser, variant);

        if (existingCart.isPresent()) {
            Cart cart = existingCart.get();
            int newQty = cart.getQuantity() + quantity;

            if (newQty > variant.getQuantityInStock()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Tổng số lượng vượt quá số lượng tồn kho");
            }

            cart.setQuantity(newQty);
            cartRepository.save(cart);
        } else {
            Cart cart = new Cart();
            cart.setMember(loggedInUser);
            cart.setVariant(variant);
            cart.setQuantity(quantity);
            cartRepository.save(cart);
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/cart/items")
    @ResponseBody
    public ResponseEntity<?> getCartItems(HttpSession session) {
        Member loggedInUser = (Member) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Chưa đăng nhập");
        }

        List<Cart> cartItems = cartRepository.findByMember(loggedInUser);

        // Duyệt và build dữ liệu đơn giản để gửi về cho client
        List<Map<String, Object>> items = new ArrayList<>();

        for (Cart cart : cartItems) {
            ProductVariant variant = cart.getVariant();
            Product product = variant.getProduct();

            Map<String, Object> item = new HashMap<>();
            item.put("variantId", variant.getVariantID()); // cần để JS gọi xóa

            item.put("productName", product.getProductName());
            item.put("storage", variant.getStorage());
            item.put("color", variant.getColor());
            item.put("image", variant.getImagesno2());
            item.put("price", variant.getDiscountedPrice() != null ? variant.getDiscountedPrice() : variant.getPrice());
            item.put("quantity", cart.getQuantity());

            items.add(item);
        }

        return ResponseEntity.ok(items);
    }

    @PostMapping("/api/cart/remove")
    @ResponseBody
    public ResponseEntity<?> removeFromCart(
            @RequestParam("variantId") Integer variantId,
            HttpSession session) {

        Member loggedInUser = (Member) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bạn cần đăng nhập");
        }

        Optional<ProductVariant> variantOpt = productVariantRepository.findById(variantId);
        if (variantOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Sản phẩm không tồn tại");
        }

        Optional<Cart> existingCart = cartRepository.findByMemberAndVariant(loggedInUser, variantOpt.get());
        if (existingCart.isPresent()) {
            cartRepository.delete(existingCart.get());
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/cart/update")
    @ResponseBody
    public ResponseEntity<?> updateCartItem(
            @RequestParam("variantId") Integer variantId,
            @RequestParam("quantity") int quantity,
            HttpSession session) {

        Member loggedInUser = (Member) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bạn cần đăng nhập");
        }

        Optional<ProductVariant> variantOpt = productVariantRepository.findById(variantId);
        if (variantOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Sản phẩm không tồn tại");
        }

        ProductVariant variant = variantOpt.get();

        if (quantity > variant.getQuantityInStock()) {
            return ResponseEntity.badRequest().body("Số lượng vượt quá tồn kho (" + variant.getQuantityInStock() + ")");
        }

        Optional<Cart> cartOpt = cartRepository.findByMemberAndVariant(loggedInUser, variant);
        if (cartOpt.isPresent()) {
            if (quantity <= 0) {
                cartRepository.delete(cartOpt.get());
            } else {
                Cart cart = cartOpt.get();
                cart.setQuantity(quantity);
                cartRepository.save(cart);
            }
        }

        return ResponseEntity.ok().build();
    }

}
