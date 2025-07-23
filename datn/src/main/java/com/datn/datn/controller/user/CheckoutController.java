package com.datn.datn.controller.user;

import com.datn.datn.model.Member;
import com.datn.datn.common.VnpayUtils;
import com.datn.datn.model.Cart;
import com.datn.datn.model.ProductVariant;
import com.datn.datn.repository.CartRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private final CartRepository cartRepository;

    public CheckoutController(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    @GetMapping()
    public String checkout(Model model, HttpSession session) {
        Object loggedInUser = session.getAttribute("loggedInUser");

        if (!(loggedInUser instanceof Member member)) {
            return "redirect:/login"; // Chuyển hướng nếu chưa đăng nhập
        }

        // Thông tin người dùng
        model.addAttribute("email", member.getEmail());
        model.addAttribute("name", member.getFullname());
        model.addAttribute("phone", member.getPhone());

        // Lấy giỏ hàng và tính tổng tiền
        List<Cart> cartItems = cartRepository.findByMember(member);
        BigDecimal total = BigDecimal.ZERO;

        for (Cart cart : cartItems) {
            ProductVariant variant = cart.getVariant();
            BigDecimal price = variant.getDiscountedPrice() != null 
                ? variant.getDiscountedPrice() 
                : BigDecimal.valueOf(variant.getPrice());
            total = total.add(price.multiply(BigDecimal.valueOf(cart.getQuantity())));
        }

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("total", total);
        model.addAttribute("loggedInUser", member);

        return "views/user/checkout";
    }
    @GetMapping("/payment")
public void payment(HttpServletResponse response, HttpSession session) throws IOException {
    Member member = (Member) session.getAttribute("loggedInUser");
    if (member == null) {
        response.sendRedirect("/login");
        return;
    }

    // Tổng tiền từ giỏ hàng
    List<Cart> cartItems = cartRepository.findByMember(member);
    BigDecimal total = cartItems.stream()
            .map(cart -> {
                BigDecimal price = cart.getVariant().getDiscountedPrice() != null
                        ? cart.getVariant().getDiscountedPrice()
                        : BigDecimal.valueOf(cart.getVariant().getPrice());
                return price.multiply(BigDecimal.valueOf(cart.getQuantity()));
})
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // Chuyển hướng sang trang VNPay
    String paymentUrl = VnpayUtils.createPaymentUrl(total, member); // bạn sẽ tạo hàm này bên dưới
    response.sendRedirect(paymentUrl);
}
@GetMapping("/return")
public void paymentReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String responseCode = request.getParameter("vnp_ResponseCode");
    System.out.println("VNPay responseCode = " + responseCode);
    
    if ("00".equals(responseCode)) {
        response.sendRedirect("/home");
    } else {
        response.sendRedirect("/checkout");
    }
}
@GetMapping("/ipn")
public void vnpayIpn(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    Map<String, String> fields = new HashMap<>();

    // Lấy toàn bộ tham số gửi về từ VNPay
    request.getParameterMap().forEach((key, values) -> {
        if (values.length > 0) {
            fields.put(key, values[0]);
        }
    });

    String vnp_HashSecret = "L0EQ58ZZ3TN5QRXXASM9WZA3SPE63NZJ";
    String vnp_SecureHash = fields.get("vnp_SecureHash");

    // Xoá SecureHash để phục vụ việc kiểm tra
    fields.remove("vnp_SecureHash");
    fields.remove("vnp_SecureHashType");

    String hashData = VnpayUtils.hashAllFields(fields);
    String myHash = VnpayUtils.hmacSHA512(vnp_HashSecret, hashData);

    if (myHash.equals(vnp_SecureHash)) {
        // Hash hợp lệ, tiến hành xử lý
        String vnp_TxnRef = fields.get("vnp_TxnRef");
        String responseCode = fields.get("vnp_ResponseCode");
        String amount = fields.get("vnp_Amount");

        System.out.println("✔ IPN nhận đơn hàng: " + vnp_TxnRef + " - Số tiền: " + amount);

        if ("00".equals(responseCode)) {
            System.out.println("✔ Giao dịch thành công");
            // TODO: Cập nhật trạng thái đơn hàng từ `vnp_TxnRef` trong DB (nếu có)
        } else {
            System.out.println("✘ Giao dịch thất bại");
        }

        response.getWriter().write("{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}");
    } else {
        System.out.println("✘ Sai chữ ký");
        response.getWriter().write("{\"RspCode\":\"97\",\"Message\":\"Invalid Checksum\"}");
    }
}

@GetMapping("/cod-confirm")
public String confirmCod(HttpSession session) {
    Member member = (Member) session.getAttribute("loggedInUser");
    if (member == null) return "redirect:/login";

    // TODO: Tạo đơn hàng và gán trạng thái "Chờ xác nhận" + phương thức thanh toán là COD
    // Có thể xóa cart nếu cần

    return "redirect:/home"; // hoặc trang cảm ơn
}


}