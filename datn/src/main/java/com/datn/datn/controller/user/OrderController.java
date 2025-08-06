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
        // Lấy member hiện tại từ session hoặc authentication
        Member currentUser = (Member) session.getAttribute("loggedInUser");
        if (currentUser != null) {
            List<Order> orders = orderService.getOrdersByMemberId(currentUser.getId());
            model.addAttribute("orders", orders);
        } else {
            model.addAttribute("orders", new ArrayList<>());
        }

        return "views/user/order"; // đường dẫn tới file Thymeleaf (hoặc JSP) hiển thị
    }

    @GetMapping("/order/{orderId}")
    public String viewOrderDetail(@PathVariable("orderId") Long orderId, Model model, HttpSession session) {
        Member member = (Member) session.getAttribute("loggedInUser");
        if (member == null) {
            return "redirect:/login";
        }

        Order order = orderService.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (order == null || !order.getMember().getId().equals(member.getId())) {
            return "redirect:/my-orders"; // hoặc trang 404
        }

        
        double shippingFee = 40000;
        model.addAttribute("discountAmount", order.getDiscountAmount() != null ? order.getDiscountAmount() : 0);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("order", order);
        model.addAttribute("orderDetails", order.getOrderDetails());

        return "views/user/orderDetail"; // Đây là trang HTML bạn đã viết
    }

}
