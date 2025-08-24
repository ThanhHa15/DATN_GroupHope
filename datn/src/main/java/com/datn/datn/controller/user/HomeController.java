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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.datn.datn.model.Cart;
import com.datn.datn.model.Member;
import com.datn.datn.model.Product;
import com.datn.datn.model.ProductVariant;
import com.datn.datn.repository.CartRepository;
import com.datn.datn.repository.MemberRepository;
import com.datn.datn.repository.ProductVariantRepository;
import com.datn.datn.service.AuthService;
import com.datn.datn.service.EmailService;
import com.datn.datn.service.MembersService;
import com.datn.datn.service.ProductVariantService;
import com.datn.datn.service.WishlistService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
            HttpServletRequest request) {

        input = input.trim().toLowerCase();
        password = password.trim();

        Optional<Member> optional = memberRepository.findByEmailOrPhone(input, input);

        if (optional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Sai email/số điện thoại hoặc mật khẩu");
            return "redirect:/login";
        }

        Member member = optional.get();

        // Kiểm tra login type
        if ("GOOGLE".equals(member.getLoginType())) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập bằng Google");
            return "redirect:/login";
        }

        // Kiểm tra mật khẩu
        if (!passwordEncoder.matches(password, member.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "Sai mật khẩu");
            return "redirect:/login";
        }

        // Kiểm tra tài khoản bị khóa
        if (!member.isActive()) {
            redirectAttributes.addFlashAttribute("error", "Tài khoản đã bị khóa");
            return "redirect:/login";
        }

        // Kiểm tra OTP (chỉ CUSTOMER mới cần)
        if ((member.getVerified() == null || !member.getVerified())
                && "CUSTOMER".equalsIgnoreCase(member.getRole())) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng xác thực OTP trước khi đăng nhập");
            redirectAttributes.addFlashAttribute("email", member.getEmail());
            return "redirect:/otp";
        }

        // Tạo session mới
        session.invalidate();
        session = request.getSession(true);
        session.setAttribute("loggedInUser", member);
        session.setAttribute("role", member.getRole()); // lưu role (ADMIN / STAFF)

        // Điều hướng theo role
        switch (member.getRole()) {
            case "ADMIN":
                return "redirect:/admin/employees";
            case "STAFF":
                return "redirect:/admin-products";
            case "CUSTOMER":
                session.setAttribute("showLoginSuccess", true);
                return "redirect:/home";
            default:
                redirectAttributes.addFlashAttribute("error", "Vai trò không hợp lệ");
                return "redirect:/login";
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

        // Mã hóa mật khẩu trước khi lưu
        String encodedPassword = passwordEncoder.encode(member.getPassword());
        member.setPassword(encodedPassword);
        member.setConfirmPassword(null); // Không cần lưu confirmPassword vào DB

        // Gán mặc định quyền và trạng thái
        member.setRole("CUSTOMER");
        member.setVerified(false);
        member.setActive(true);

        // Tạo và gán OTP
        String otp = authService.generateOtp();
        member.setOtp(otp);
        member.setLoginType("SYSTEM");

        // Lưu thông tin member
        memberRepository.save(member);

        // Gửi mã OTP
        authService.sendOtp(member.getEmail());

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
                return "redirect:/login";
            } else {
                redirectAttributes.addFlashAttribute("error", "Mã OTP không hợp lệ");
                redirectAttributes.addFlashAttribute("email", email); // Giữ lại email
                return "redirect:/otp";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi hệ thống khi xác thực OTP: " + e.getMessage());
            redirectAttributes.addFlashAttribute("email", email); // Giữ lại email
            return "redirect:/otp";
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

        // Sử dụng Optional để xử lý trường hợp không tìm thấy email
        Optional<Member> optionalMember = memberService.findByEmail(email);

        if (optionalMember.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Email không tồn tại!");
            return "redirect:/forgetPass";
        }

        Member member = optionalMember.get();

        // Tạo mật khẩu mới và mã hóa bằng BCrypt
        String newRawPassword = UUID.randomUUID().toString().substring(0, 8);
        String encodedPassword = passwordEncoder.encode(newRawPassword);

        member.setPassword(encodedPassword); // Lưu mật khẩu đã mã hóa
        memberService.save(member);

        // In ra console (chỉ để test) - Nên xóa trong production
        // System.out.println("Mật khẩu mới của {} là: {}", email, newRawPassword);

        // Gửi email chứa mật khẩu mới (chưa mã hóa) cho người dùng
        emailService.sendNewPassword(email, newRawPassword);

        redirectAttributes.addFlashAttribute("message", "Mật khẩu mới đã được gửi đến email của bạn!");
        return "redirect:/forgetPass";
    }

    @GetMapping("/detail")
    public String detail(Model model, HttpSession session) {
        return "views/user/products-detail";
    }

    @PostMapping("/delete-account")
    @ResponseBody
    public ResponseEntity<?> deleteAccount(HttpSession session, RedirectAttributes redirectAttributes) {
        Member loggedInUser = (Member) session.getAttribute("loggedInUser");

        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bạn cần đăng nhập để thực hiện hành động này");
        }

        try {
            // Lấy user từ database để đảm bảo dữ liệu mới nhất
            Member currentUser = memberService.getEmployeeById(loggedInUser.getId());

            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy tài khoản");
            }

            // Kiểm tra quyền (chỉ cho phép CUSTOMER xóa tài khoản của chính mình)
            if (!"CUSTOMER".equals(currentUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không có quyền thực hiện hành động này");
            }

            // Xóa các dữ liệu liên quan trước khi xóa user
            // Xóa giỏ hàng
            List<Cart> cartItems = cartRepository.findByMember(currentUser);
            cartRepository.deleteAll(cartItems);

            // Xóa wishlist
            // (Nếu có service xóa wishlist)

            // Xóa user
            memberRepository.delete(currentUser);

            // Hủy session
            session.invalidate();

            log.info("User account deleted successfully: {}", currentUser.getEmail());

            return ResponseEntity.ok().body("Tài khoản đã được xóa thành công");

        } catch (Exception e) {
            log.error("Error deleting user account: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Có lỗi xảy ra khi xóa tài khoản: " + e.getMessage());
        }
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

    @GetMapping("/localate")
    public String locolate(Model model, HttpSession session) {
        Member user = (Member) session.getAttribute("loggedInUser");
        model.addAttribute("loggedInUser", user);
        return "views/shared/localate";
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
        Member sessionUser = (Member) session.getAttribute("loggedInUser");
        if (sessionUser == null) {
            return "redirect:/login";
        }

        // ✅ Lấy bản mới từ DB để Hibernate quản lý entity đúng cách
        Member currentUser = memberService.getEmployeeById(sessionUser.getId());

        if (currentUser == null) {
            return "redirect:/login";
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
            if (!currentPassword.equals(sessionUser.getPassword())) {
                model.addAttribute("error", "Mật khẩu hiện tại không đúng");
                model.addAttribute("member", sessionUser);
                return "views/shared/editInf";
            }

            if (newPassword == null || newPassword.isBlank()) {
                model.addAttribute("error", "Vui lòng nhập mật khẩu mới");
                model.addAttribute("member", sessionUser);
                return "views/shared/editInf";
            }

            if (!newPassword.equals(confirmPassword)) {
                model.addAttribute("error", "Mật khẩu xác nhận không khớp");
                model.addAttribute("member", sessionUser);
                return "views/shared/editInf";
            }

            currentUser.setPassword(newPassword); // Hash nếu cần
        }

        // ✅ Save vào DB
        memberService.save(currentUser);

        // ✅ Cập nhật lại session
        session.setAttribute("loggedInUser", currentUser);

        model.addAttribute("success", "Cập nhật thông tin thành công");
        model.addAttribute("member", currentUser);
        return "views/shared/editInf";
    }

    @GetMapping("/updateif")
    public String showUpdateForm(HttpSession session, Model model) {
        Member currentUser = (Member) session.getAttribute("loggedInUser");
        if (currentUser == null)
            return "redirect:/login";

        model.addAttribute("member", currentUser);
        return "views/shared/updateif"; // tên view hiển thị form
    }

    @PostMapping("/updateif")
    public String updateProfile(HttpServletRequest request, HttpSession session, Model model) {
        Member sessionUser = (Member) session.getAttribute("loggedInUser");
        if (sessionUser == null) {
            return "redirect:/login";
        }

        Member currentUser = memberService.getEmployeeById(sessionUser.getId());
        if (currentUser == null) {
            return "redirect:/login";
        }

        // Lấy dữ liệu từ form
        String fullname = request.getParameter("fullname");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String birthdayStr = request.getParameter("birthday");

        // Validate đơn giản (nếu muốn kiểm tra thêm)
        if (fullname == null || fullname.isBlank() ||
                phone == null || phone.isBlank()) {
            model.addAttribute("error", "Vui lòng nhập đầy đủ thông tin");
            model.addAttribute("member", currentUser);
            return "views/shared/updateif";
        }

        // Cập nhật dữ liệu
        currentUser.setFullname(fullname);
        currentUser.setEmail(email); // nếu bạn cho phép sửa email
        currentUser.setPhone(phone);

        if (birthdayStr != null && !birthdayStr.isBlank()) {
            try {
                currentUser.setBirthday(LocalDate.parse(birthdayStr));
            } catch (Exception e) {
                model.addAttribute("error", "Ngày sinh không hợp lệ");
                model.addAttribute("member", currentUser);
                return "views/shared/login";
            }
        }

        // Lưu và cập nhật session
        memberService.save(currentUser);
        session.setAttribute("loggedInUser", currentUser);

        model.addAttribute("success", "Cập nhật thông tin thành công");
        model.addAttribute("member", currentUser);
        return "redirect:/"; // ✅ đúng nếu view của bạn là file editInf.html
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
