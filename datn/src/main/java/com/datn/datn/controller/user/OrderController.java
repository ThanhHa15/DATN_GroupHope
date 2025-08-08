package com.datn.datn.controller.user;

import java.util.ArrayList;
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
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/order")
    public String getOrders(Model model, HttpSession session) {
        Member currentUser = (Member) session.getAttribute("loggedInUser");
        if (currentUser != null) {
            List<Order> orders = orderService.getOrdersByMemberId(currentUser.getId());

            // Đảm bảo tính toán tổng giá trị nếu cần
            orders.forEach(order -> {
                if (order.getTotalPrice() == null) {
                    // Tính toán tổng giá trị từ orderDetails nếu totalPrice null
                    double total = order.getOrderDetails().stream()
                            .mapToDouble(detail -> detail.getPrice() * detail.getQuantity())
                            .sum();
                    order.setTotalPrice(total);
                }
            });

            model.addAttribute("orders", orders);
        } else {
            model.addAttribute("orders", new ArrayList<>());
        }
        return "views/user/order";
    }

    @GetMapping("/order/{orderId}")
    public String viewOrderDetail(@PathVariable("orderId") Long orderId, Model model, HttpSession session) {
        Member member = (Member) session.getAttribute("loggedInUser");
        if (member == null) {
            return "redirect:/login";
        }

        Order order = orderService.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (!order.getMember().getId().equals(member.getId())) {
            return "redirect:/my-orders"; // hoặc trang 404
        }

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

        return "views/user/orderDetail";
    }

}