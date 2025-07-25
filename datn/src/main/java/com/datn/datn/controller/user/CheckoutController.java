package com.datn.datn.controller.user;

import com.datn.datn.model.Member;
import com.datn.datn.model.Order;
import com.datn.datn.common.VnpayUtils;
import com.datn.datn.model.Cart;
import com.datn.datn.model.ProductVariant;
import com.datn.datn.model.Vouchers;
import com.datn.datn.repository.CartRepository;
import com.datn.datn.repository.ProductVariantRepository;
import com.datn.datn.repository.VoucherRepository;
import com.datn.datn.service.OrderService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private final CartRepository cartRepository;

     private final ProductVariantRepository productVariantRepository;

     private final OrderService orderService;  
     private final VoucherRepository voucherRepository;
    public CheckoutController(CartRepository cartRepository,ProductVariantRepository productVariantRepository,OrderService orderService,VoucherRepository voucherRepository) {
        this.cartRepository = cartRepository;
        this.productVariantRepository = productVariantRepository;
        this.orderService = orderService;
        this.voucherRepository = voucherRepository;
    }

    @GetMapping()
public String checkout(@RequestParam(value = "voucherCode", required = false) String voucherCode,
                       Model model, HttpSession session) {
    Object loggedInUser = session.getAttribute("loggedInUser");

    if (!(loggedInUser instanceof Member member)) {
        return "redirect:/login";
    }

    model.addAttribute("email", member.getEmail());
    model.addAttribute("name", member.getFullname());
    model.addAttribute("phone", member.getPhone());

    List<Cart> cartItems = cartRepository.findByMember(member);
    BigDecimal total = BigDecimal.ZERO;
    BigDecimal originalTotal = BigDecimal.ZERO;

    for (Cart cart : cartItems) {
        ProductVariant variant = cart.getVariant();
        BigDecimal originalPrice = BigDecimal.valueOf(variant.getPrice());
        BigDecimal price = variant.getDiscountedPrice() != null 
            ? variant.getDiscountedPrice() 
            : originalPrice;

        BigDecimal quantity = BigDecimal.valueOf(cart.getQuantity());
        originalTotal = originalTotal.add(originalPrice.multiply(quantity));
        total = total.add(price.multiply(quantity));
    }

    model.addAttribute("cartItems", cartItems);
    model.addAttribute("loggedInUser", member);
    model.addAttribute("originalTotal", originalTotal); // ✅ Thêm dòng này

    if (voucherCode != null && !voucherCode.trim().isEmpty()) {
        Vouchers voucher = voucherRepository.findByCode(voucherCode.trim()).orElse(null);
        if (voucher != null
                && voucher.getQuantity() > 0
                && voucher.getStartDate().isBefore(java.time.LocalDate.now().plusDays(1))
                && voucher.getEndDate().isAfter(java.time.LocalDate.now().minusDays(1))
                && total.compareTo(voucher.getMinimumPrice()) >= 0) {

            BigDecimal discount = total.multiply(BigDecimal.valueOf(voucher.getDiscountPercent()))
    .divide(BigDecimal.valueOf(100), 1, RoundingMode.HALF_UP);

            total = total.subtract(discount);

            session.setAttribute("appliedVoucher", voucher);
            model.addAttribute("discountAmount", discount);
            model.addAttribute("voucherCode", voucherCode);
        } else {
            model.addAttribute("voucherError", "Mã giảm giá không hợp lệ hoặc không đủ điều kiện.");
        }
    }

    model.addAttribute("total", total);
    return "views/user/checkout";
}
    @PostMapping("/save-address")
    public void saveAddress(@RequestParam("address") String address,
                            @RequestParam(value = "note", required = false) String note,
                            HttpSession session) {
        session.setAttribute("checkoutAddress", address);
        session.setAttribute("checkoutNote", note);
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

        // Áp dụng giảm giá nếu có
        Vouchers voucher = (Vouchers) session.getAttribute("appliedVoucher");
        if (voucher != null
                && voucher.getQuantity() > 0
                && voucher.getStartDate().isBefore(LocalDateTime.now().toLocalDate().plusDays(1))
                && voucher.getEndDate().isAfter(LocalDateTime.now().toLocalDate().minusDays(1))
                && total.compareTo(voucher.getMinimumPrice()) >= 0) {

            BigDecimal discount = total.multiply(BigDecimal.valueOf(voucher.getDiscountPercent()))
                                    .divide(BigDecimal.valueOf(100));
            total = total.subtract(discount);
        }

    // Gửi đến VNPay với số tiền đã giảm giá
    String paymentUrl = VnpayUtils.createPaymentUrl(total, member);
    response.sendRedirect(paymentUrl);
}
    @GetMapping("/return")
public void paymentReturn(HttpServletRequest request, HttpServletResponse response, HttpSession session) throws IOException {
    String responseCode = request.getParameter("vnp_ResponseCode");
    System.out.println("VNPay responseCode = " + responseCode);

    Member member = (Member) session.getAttribute("loggedInUser");
    if (!"00".equals(responseCode) || member == null) {
        response.sendRedirect("/checkout");
        return;
    }

    String address = (String) session.getAttribute("checkoutAddress");
    String note = (String) session.getAttribute("checkoutNote");

    List<Cart> cartItems = cartRepository.findByMember(member);

    // Tính tổng giỏ hàng
    BigDecimal total = BigDecimal.ZERO;
    for (Cart cart : cartItems) {
        ProductVariant variant = cart.getVariant();
        BigDecimal price = variant.getDiscountedPrice() != null
                ? variant.getDiscountedPrice()
                : BigDecimal.valueOf(variant.getPrice());
        total = total.add(price.multiply(BigDecimal.valueOf(cart.getQuantity())));
    }

    // Áp dụng giảm giá nếu có
    Vouchers voucher = (Vouchers) session.getAttribute("appliedVoucher");
    BigDecimal discountAmount = BigDecimal.ZERO;
    if (voucher != null
            && voucher.getQuantity() > 0
            && voucher.getStartDate().isBefore(LocalDateTime.now().toLocalDate().plusDays(1))
            && voucher.getEndDate().isAfter(LocalDateTime.now().toLocalDate().minusDays(1))
            && total.compareTo(voucher.getMinimumPrice()) >= 0) {

        discountAmount = total.multiply(BigDecimal.valueOf(voucher.getDiscountPercent()))
                              .divide(BigDecimal.valueOf(100));
        total = total.subtract(discountAmount);
    }

    for (Cart cart : cartItems) {
        ProductVariant variant = cart.getVariant();
        int purchaseQuantity = cart.getQuantity();

        // Trừ tồn kho
        variant.setQuantityInStock(variant.getQuantityInStock() - purchaseQuantity);
        productVariantRepository.save(variant);

        // Tính giá từng item
        BigDecimal price = variant.getDiscountedPrice() != null
                ? variant.getDiscountedPrice()
                : BigDecimal.valueOf(variant.getPrice());

        BigDecimal itemSubtotal = price.multiply(BigDecimal.valueOf(purchaseQuantity));

        // Phân bổ giảm giá
        BigDecimal itemDiscount = BigDecimal.ZERO;
        if (discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            itemDiscount = itemSubtotal.multiply(discountAmount).divide(total.add(discountAmount), 10, BigDecimal.ROUND_HALF_UP);
        }

        BigDecimal itemTotal = itemSubtotal.subtract(itemDiscount);

        // Lưu order
        Order order = new Order();
        order.setMember(member);
        order.setProductVariant(variant);
        order.setOrderDate(LocalDateTime.now());
        order.setAddress(address != null ? address : "Không có địa chỉ");
        order.setNote(note);
        order.setTotalPrice(itemTotal.setScale(1, RoundingMode.HALF_UP).doubleValue());

        orderService.save(order);
    }

    // Giảm số lượng voucher
    if (voucher != null) {
        voucher.setQuantity(voucher.getQuantity() - 1);
        voucherRepository.save(voucher);
        session.removeAttribute("appliedVoucher");
    }

    cartRepository.deleteAll(cartItems);
    session.removeAttribute("checkoutAddress");
    session.removeAttribute("checkoutNote");
    response.sendRedirect("/home");
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

    String address = (String) session.getAttribute("checkoutAddress");
    String note = (String) session.getAttribute("checkoutNote");

    List<Cart> cartItems = cartRepository.findByMember(member);

    // Tính tổng
    BigDecimal total = BigDecimal.ZERO;
    for (Cart cart : cartItems) {
        ProductVariant variant = cart.getVariant();
        BigDecimal price = variant.getDiscountedPrice() != null
                ? variant.getDiscountedPrice()
                : BigDecimal.valueOf(variant.getPrice());
        total = total.add(price.multiply(BigDecimal.valueOf(cart.getQuantity())));
    }

    // Áp dụng giảm giá
    Vouchers voucher = (Vouchers) session.getAttribute("appliedVoucher");
    BigDecimal discountAmount = BigDecimal.ZERO;
    if (voucher != null
            && voucher.getQuantity() > 0
            && voucher.getStartDate().isBefore(LocalDateTime.now().toLocalDate().plusDays(1))
            && voucher.getEndDate().isAfter(LocalDateTime.now().toLocalDate().minusDays(1))
            && total.compareTo(voucher.getMinimumPrice()) >= 0) {

        discountAmount = total.multiply(BigDecimal.valueOf(voucher.getDiscountPercent()))
                              .divide(BigDecimal.valueOf(100));
        total = total.subtract(discountAmount);
    }

    for (Cart cart : cartItems) {
        ProductVariant variant = cart.getVariant();
        int purchaseQuantity = cart.getQuantity();

        // Trừ tồn kho
        variant.setQuantityInStock(variant.getQuantityInStock() - purchaseQuantity);
        productVariantRepository.save(variant);

        // Tính giá từng item
        BigDecimal price = variant.getDiscountedPrice() != null
                ? variant.getDiscountedPrice()
                : BigDecimal.valueOf(variant.getPrice());

        BigDecimal itemSubtotal = price.multiply(BigDecimal.valueOf(purchaseQuantity));

        // Phân bổ giảm giá
        BigDecimal itemDiscount = BigDecimal.ZERO;
        if (discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            itemDiscount = itemSubtotal.multiply(discountAmount).divide(total.add(discountAmount), 10, BigDecimal.ROUND_HALF_UP);
        }

        BigDecimal itemTotal = itemSubtotal.subtract(itemDiscount);

        // Lưu order
        Order order = new Order();
        order.setMember(member);
        order.setProductVariant(variant);
        order.setOrderDate(LocalDateTime.now());
        order.setAddress(address != null ? address : "Không có địa chỉ");
        order.setNote(note);
        order.setTotalPrice(itemTotal.setScale(1, RoundingMode.HALF_UP).doubleValue());

        orderService.save(order);
    }

    // Giảm số lượng voucher
    if (voucher != null) {
        voucher.setQuantity(voucher.getQuantity() - 1);
        voucherRepository.save(voucher);
        session.removeAttribute("appliedVoucher");
    }

    cartRepository.deleteAll(cartItems);
    session.removeAttribute("checkoutAddress");
    session.removeAttribute("checkoutNote");

    return "redirect:/home";
}


}