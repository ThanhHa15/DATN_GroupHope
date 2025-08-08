package com.datn.datn.controller.user;

import com.datn.datn.model.Member;
import com.datn.datn.model.Order;
import com.datn.datn.model.OrderDetail;
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
import java.util.ArrayList;
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

    public CheckoutController(CartRepository cartRepository, ProductVariantRepository productVariantRepository,
            OrderService orderService, VoucherRepository voucherRepository) {
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
        List<String> savedAddresses = orderService.getMemberAddresses(member.getId());
        model.addAttribute("savedAddresses", savedAddresses);

        String lastUsedAddress = !savedAddresses.isEmpty() ? savedAddresses.get(savedAddresses.size() - 1) : null;
        model.addAttribute("lastUsedAddress", lastUsedAddress);

        model.addAttribute("email", member.getEmail());
        model.addAttribute("name", member.getFullname());
        model.addAttribute("phone", member.getPhone());

        List<Cart> cartItems = cartRepository.findByMember(member);
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal originalTotal = BigDecimal.ZERO;

        for (Cart cart : cartItems) {
            ProductVariant variant = cart.getVariant();
            BigDecimal originalPrice = BigDecimal.valueOf(variant.getPrice());
            BigDecimal price = variant.getDiscountedPrice() != null ? variant.getDiscountedPrice() : originalPrice;
            BigDecimal quantity = BigDecimal.valueOf(cart.getQuantity());

            originalTotal = originalTotal.add(originalPrice.multiply(quantity));
            total = total.add(price.multiply(quantity));
        }

        BigDecimal shippingFee = BigDecimal.valueOf(40000);
        BigDecimal grandTotal = originalTotal.add(shippingFee); // Sử dụng originalTotal thay vì total

        BigDecimal discount = BigDecimal.ZERO;
        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            Vouchers voucher = voucherRepository.findByCode(voucherCode.trim()).orElse(null);
            if (voucher != null
                    && voucher.getQuantity() > 0
                    && voucher.getStartDate().isBefore(java.time.LocalDate.now().plusDays(1))
                    && voucher.getEndDate().isAfter(java.time.LocalDate.now().minusDays(1))
                    && grandTotal.compareTo(voucher.getMinimumPrice()) >= 0) {

                // Áp dụng giảm giá trên grandTotal (originalTotal + shippingFee)
                discount = grandTotal.multiply(BigDecimal.valueOf(voucher.getDiscountPercent()))
                        .divide(BigDecimal.valueOf(100), 1, RoundingMode.HALF_UP);
                grandTotal = grandTotal.subtract(discount);

                session.setAttribute("appliedVoucher", voucher);
                model.addAttribute("discountAmount", discount);
                model.addAttribute("voucherCode", voucherCode);
            } else {
                model.addAttribute("voucherError", "Mã giảm giá không hợp lệ hoặc không đủ điều kiện.");
            }
        }

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("loggedInUser", member);
        model.addAttribute("originalTotal", originalTotal);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("total", grandTotal);

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

        List<Cart> cartItems = cartRepository.findByMember(member);
        BigDecimal originalTotal = cartItems.stream()
                .map(cart -> BigDecimal.valueOf(cart.getVariant().getPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal shippingFee = BigDecimal.valueOf(40000);
        BigDecimal grandTotal = originalTotal.add(shippingFee);

        Vouchers voucher = (Vouchers) session.getAttribute("appliedVoucher");
        if (voucher != null
                && voucher.getQuantity() > 0
                && voucher.getStartDate().isBefore(LocalDateTime.now().toLocalDate().plusDays(1))
                && voucher.getEndDate().isAfter(LocalDateTime.now().toLocalDate().minusDays(1))
                && grandTotal.compareTo(voucher.getMinimumPrice()) >= 0) {

            BigDecimal discount = grandTotal.multiply(BigDecimal.valueOf(voucher.getDiscountPercent()))
                    .divide(BigDecimal.valueOf(100));
            grandTotal = grandTotal.subtract(discount);
        }

        String paymentUrl = VnpayUtils.createPaymentUrl(grandTotal, member);
        response.sendRedirect(paymentUrl);
    }

    @GetMapping("/return")
    public void paymentReturn(HttpServletRequest request, HttpServletResponse response, HttpSession session)
            throws IOException {
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

        BigDecimal shippingFee = BigDecimal.valueOf(40000);
        BigDecimal totalBeforeDiscount = BigDecimal.ZERO;

        for (Cart cart : cartItems) {
            ProductVariant variant = cart.getVariant();
            BigDecimal price = variant.getDiscountedPrice() != null
                    ? variant.getDiscountedPrice()
                    : BigDecimal.valueOf(variant.getPrice());
            totalBeforeDiscount = totalBeforeDiscount.add(price.multiply(BigDecimal.valueOf(cart.getQuantity())));
        }

        Vouchers voucher = (Vouchers) session.getAttribute("appliedVoucher");
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (voucher != null
                && voucher.getQuantity() > 0
                && voucher.getStartDate().isBefore(LocalDateTime.now().toLocalDate().plusDays(1))
                && voucher.getEndDate().isAfter(LocalDateTime.now().toLocalDate().minusDays(1))
                && totalBeforeDiscount.compareTo(voucher.getMinimumPrice()) >= 0) {
            discountAmount = totalBeforeDiscount.add(shippingFee)
                    .multiply(BigDecimal.valueOf(voucher.getDiscountPercent()))
                    .divide(BigDecimal.valueOf(100), 1, RoundingMode.HALF_UP);
        }

        BigDecimal grandTotal = totalBeforeDiscount.add(shippingFee).subtract(discountAmount);
        BigDecimal totalAllocated = BigDecimal.ZERO;
        int index = 0;

        for (Cart cart : cartItems) {
            ProductVariant variant = cart.getVariant();
            int qty = cart.getQuantity();
            BigDecimal price = variant.getDiscountedPrice() != null
                    ? variant.getDiscountedPrice()
                    : BigDecimal.valueOf(variant.getPrice());
            BigDecimal itemSubtotal = price.multiply(BigDecimal.valueOf(qty));
            BigDecimal ratio = itemSubtotal.divide(totalBeforeDiscount, 10, RoundingMode.HALF_UP);
            BigDecimal itemTotal = grandTotal.multiply(ratio).setScale(1, RoundingMode.HALF_UP);
            totalAllocated = totalAllocated.add(itemTotal);

            if (++index == cartItems.size()) {
                itemTotal = itemTotal.add(grandTotal.subtract(totalAllocated));
            }

            variant.setQuantityInStock(variant.getQuantityInStock() - qty);
            productVariantRepository.save(variant);

            Order order = new Order();
            order.setMember(member);
            order.setOrderDate(LocalDateTime.now());
            order.setAddress(address != null ? address : "Không có địa chỉ");
            order.setNote(note);
            order.setTotalPrice(itemTotal.doubleValue());
            order.setPaymentStatus("Đã thanh toán");
            order.setOrderStatus("Chờ xác nhận");
            order.setDiscountAmount(discountAmount.doubleValue());

            Order savedOrder = orderService.save(order);

            List<OrderDetail> orderDetails = new ArrayList<>();
            for (Cart cartItem : cartItems) {
                ProductVariant varianteee = cartItem.getVariant();
                int qtyseee = cartItem.getQuantity();

                // Cập nhật số lượng tồn kho
                varianteee.setQuantityInStock(varianteee.getQuantityInStock() - qtyseee);
                productVariantRepository.save(varianteee);

                // Tạo chi tiết đơn hàng
                OrderDetail orderDetail = new OrderDetail();
                orderDetail.setOrder(savedOrder);
                orderDetail.setProductVariant(varianteee);
                orderDetail.setQuantity(qtyseee);
                orderDetail.setPrice(
                        varianteee.getDiscountedPrice() != null ? varianteee.getDiscountedPrice().doubleValue()
                                : varianteee.getPrice());

                orderDetails.add(orderDetail);
            }
            savedOrder.setOrderDetails(orderDetails);
            orderService.save(savedOrder);
        }

        if (voucher != null) {
            voucher.setQuantity(voucher.getQuantity() - 1);
            voucherRepository.save(voucher);
            session.removeAttribute("appliedVoucher");
        }

        cartRepository.deleteAll(cartItems);
        session.removeAttribute("checkoutAddress");
        session.removeAttribute("checkoutNote");

        session.setAttribute("paymentMethod", "Chuyển khoản");
        response.sendRedirect("/result-order");

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
    if (member == null)
        return "redirect:/login";

    String address = (String) session.getAttribute("checkoutAddress");
    String note = (String) session.getAttribute("checkoutNote");

    List<Cart> cartItems = cartRepository.findByMember(member);

    BigDecimal shippingFee = BigDecimal.valueOf(40000);
    BigDecimal totalBeforeDiscount = BigDecimal.ZERO;

    // Tính tổng giá trị đơn hàng trước giảm giá
    for (Cart cart : cartItems) {
        ProductVariant variant = cart.getVariant();
        BigDecimal price = variant.getDiscountedPrice() != null
                ? variant.getDiscountedPrice()
                : BigDecimal.valueOf(variant.getPrice());
        totalBeforeDiscount = totalBeforeDiscount.add(price.multiply(BigDecimal.valueOf(cart.getQuantity())));
    }

    // Tính toán giảm giá nếu có voucher
    Vouchers voucher = (Vouchers) session.getAttribute("appliedVoucher");
    BigDecimal discountAmount = BigDecimal.ZERO;
    if (voucher != null
            && voucher.getQuantity() > 0
            && voucher.getStartDate().isBefore(LocalDateTime.now().toLocalDate().plusDays(1))
            && voucher.getEndDate().isAfter(LocalDateTime.now().toLocalDate().minusDays(1))
            && totalBeforeDiscount.compareTo(voucher.getMinimumPrice()) >= 0) {
        discountAmount = totalBeforeDiscount.add(shippingFee)
                .multiply(BigDecimal.valueOf(voucher.getDiscountPercent()))
                .divide(BigDecimal.valueOf(100), 1, RoundingMode.HALF_UP);
    }

    // Tính tổng giá trị cuối cùng
    BigDecimal grandTotal = totalBeforeDiscount.add(shippingFee).subtract(discountAmount);

    // Tạo đơn hàng duy nhất
    Order order = new Order();
    order.setMember(member);
    order.setOrderDate(LocalDateTime.now());
    order.setAddress(address != null ? address : "Không có địa chỉ");
    order.setNote(note);
    order.setTotalPrice(grandTotal.doubleValue());
    order.setPaymentStatus("Chưa thanh toán");
    order.setOrderStatus("Chờ xác nhận");
    order.setDiscountAmount(discountAmount.doubleValue());

    // Tạo danh sách chi tiết đơn hàng
    List<OrderDetail> orderDetails = new ArrayList<>();
    for (Cart cart : cartItems) {
        ProductVariant variant = cart.getVariant();
        int qty = cart.getQuantity();

        // Cập nhật số lượng tồn kho
        variant.setQuantityInStock(variant.getQuantityInStock() - qty);
        productVariantRepository.save(variant);

        // Tạo chi tiết đơn hàng
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrder(order);
        orderDetail.setProductVariant(variant);
        orderDetail.setQuantity(qty);
        orderDetail.setPrice(variant.getDiscountedPrice() != null 
                ? variant.getDiscountedPrice().doubleValue()
                : variant.getPrice());

        orderDetails.add(orderDetail);
    }

    // Liên kết orderDetails với order và lưu
    order.setOrderDetails(orderDetails);
    Order savedOrder = orderService.save(order);

    // Xử lý voucher nếu có
    if (voucher != null) {
        voucher.setQuantity(voucher.getQuantity() - 1);
        voucherRepository.save(voucher);
        session.removeAttribute("appliedVoucher");
    }

    // Xóa giỏ hàng và dọn dẹp session
    cartRepository.deleteAll(cartItems);
    session.removeAttribute("checkoutAddress");
    session.removeAttribute("checkoutNote");
    session.setAttribute("paymentMethod", "Thanh toán khi nhận hàng (COD)");

    return "redirect:/result-order";
}

}