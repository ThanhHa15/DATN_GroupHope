package com.datn.datn.controller.admin;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.datn.datn.model.Member;
import com.datn.datn.model.Order;
import com.datn.datn.service.OrderService;

import jakarta.servlet.http.HttpSession;

@Controller
public class OrderAdminController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/admin-order")
    public String getAllOrders(Model model) {
        List<Order> orders = orderService.findAll();

        // Danh sách chứa dữ liệu hiển thị kèm tính toán
        List<Map<String, Object>> orderDataList = new ArrayList<>();

        for (Order order : orders) {
            Map<String, Object> data = new HashMap<>();
            double shippingFee = 40000;

            // 1. Tính giá gốc
            double productTotal = order.getOrderDetails().stream()
                .mapToDouble(detail -> detail.getPrice() * detail.getQuantity())
                .sum();

            // 2. Lấy khuyến mãi
            double discountAmount = order.getDiscountAmount() != null ? order.getDiscountAmount() : 0;

            // 3. Giá cuối
            double grandTotal = productTotal + shippingFee - discountAmount;
            if (grandTotal < 0)
                grandTotal = 0;

            // 4. Đưa tất cả vào map
            data.put("order", order);
            data.put("productTotal", productTotal);
            data.put("discountAmount", discountAmount);
            data.put("grandTotal", grandTotal);

            orderDataList.add(data);
        }

        model.addAttribute("orderDataList", orderDataList);
        return "formOrder";
    }

    @GetMapping("/admin-orderdetail/{orderId}")
    public String viewOrderDetail(@PathVariable("orderId") Long orderId, Model model) {
        Order order = orderService.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // Tính tổng tiền sản phẩm
        double productTotal = order.getOrderDetails().stream()
                .mapToDouble(detail -> detail.getPrice() * detail.getQuantity())
                .sum();
        model.addAttribute("productTotal", productTotal);

        // Phí vận chuyển
        double shippingFee = 40000;
        model.addAttribute("shippingFee", shippingFee);

        // Giảm giá
        double discountAmount = order.getDiscountAmount() != null ? order.getDiscountAmount() : 0;
        model.addAttribute("discountAmount", discountAmount);

        // Tạm tính (trước khi giảm giá)
        model.addAttribute("subtotal", productTotal + shippingFee);

        // Tổng cộng (sau giảm giá)
        double grandTotal = productTotal + shippingFee - discountAmount;
        model.addAttribute("grandTotal", grandTotal);

        // Các dữ liệu khác
        model.addAttribute("order", order);
        model.addAttribute("orderDetails", order.getOrderDetails());

        return "formOrder-Detail"; // HTML chi tiết đơn hàng admin
    }
}
