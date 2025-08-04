package com.datn.datn.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;

import com.datn.datn.model.Member;
import com.datn.datn.model.Order;
import com.datn.datn.model.OrderDetail;
import com.datn.datn.service.OrderService;
import com.datn.datn.service.OrderDetailService;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@RequestMapping("/result-order")
@Controller
public class ResultOrder {

    private final OrderService orderService;
    private final OrderDetailService orderDetailService;

    @Autowired
    public ResultOrder(OrderService orderService, OrderDetailService orderDetailService) {
        this.orderService = orderService;
        this.orderDetailService = orderDetailService;
    }

    @GetMapping
    public String result(Model model, HttpSession session) {
        Member member = (Member) session.getAttribute("loggedInUser");
        if (member == null) return "redirect:/login";
        
        // Lấy đơn hàng mới nhất của member
        List<Order> latestOrders = orderService.findLatestOrdersByMember(member);
        if (!latestOrders.isEmpty()) {
            Order latestOrder = latestOrders.get(0);
            List<OrderDetail> orderDetails = orderDetailService.findByOrder(latestOrder);
            
            model.addAttribute("order", latestOrder);
            model.addAttribute("orderDetails", orderDetails);
            
            // Tính tổng tiền sản phẩm
            double productTotal = orderDetails.stream()
                .mapToDouble(detail -> detail.getPrice() * detail.getQuantity())
                .sum();
            model.addAttribute("productTotal", productTotal);
            
            // Phí vận chuyển
            double shippingFee = 40000;
            model.addAttribute("shippingFee", shippingFee);
            
            // Tổng cộng
            double grandTotal = productTotal + shippingFee;
            model.addAttribute("grandTotal", grandTotal);
        }
        
        model.addAttribute("email", member.getEmail());
        model.addAttribute("name", member.getFullname());
        model.addAttribute("phone", member.getPhone());
        model.addAttribute("member", member);
        
        String paymentMethod = (String) session.getAttribute("paymentMethod");
        model.addAttribute("paymentMethod", paymentMethod != null ? paymentMethod : "Chuyển khoản");
        
        return "resultCheckout";
    }
}