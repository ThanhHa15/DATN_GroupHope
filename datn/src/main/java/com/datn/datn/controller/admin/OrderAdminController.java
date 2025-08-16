package com.datn.datn.controller.admin;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.datn.datn.model.Order;
import com.datn.datn.service.OrderService;

@Controller
public class OrderAdminController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/admin-order")
    public String getAllOrders(Model model) {
        List<Order> orders = orderService.findAll();
        List<Map<String, Object>> orderDataList = new ArrayList<>();

        for (Order order : orders) {
            Map<String, Object> data = new HashMap<>();
            double shippingFee = 40000; // Giả định phí vận chuyển cố định

            double productTotal = order.getTotalPrice() - shippingFee
                    + (order.getDiscountAmount() != null ? order.getDiscountAmount() : 0);
            double discountAmount = order.getDiscountAmount() != null ? order.getDiscountAmount() : 0;
            double grandTotal = order.getTotalPrice();

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

        double shippingFee = 40000;
        double productTotal = order.getTotalPrice() - shippingFee
                + (order.getDiscountAmount() != null ? order.getDiscountAmount() : 0);
        double discountAmount = order.getDiscountAmount() != null ? order.getDiscountAmount() : 0;

        model.addAttribute("order", order);
        model.addAttribute("orderDetails", order.getOrderDetails());
        model.addAttribute("productTotal", productTotal);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("discountAmount", discountAmount);
        model.addAttribute("subtotal", productTotal + shippingFee);
        model.addAttribute("grandTotal", order.getTotalPrice());

        return "formOrder-Detail";
    }

    @GetMapping("/admin-order/mark-as-paid/{orderId}")
    public String markOrderAsPaid(@PathVariable("orderId") Long orderId) {
        Order order = orderService.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        order.setPaymentStatus("Đã thanh toán");
        orderService.save(order);

        return "redirect:/admin-orderdetail/" + orderId;
    }

    @PostMapping("/admin-order/confirm-refund/{orderId}")
    public String confirmRefund(
            @PathVariable("orderId") Long orderId,
            RedirectAttributes redirectAttributes) {

        Order order = orderService.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        order.setPaymentMethod("Đã hoàn tiền");
        order.setOrderStatus("Đã giao hàng");
        order.setRefundStatus("Đã hoàn tiền");
        orderService.save(order);

        redirectAttributes.addFlashAttribute("successMessage", "Đã xác nhận hoàn tiền thành công!");
        return "redirect:/admin-orderdetail/" + orderId;
    }

    @PostMapping("/admin-order/update-status/{orderId}")
    public String updateOrderStatus(
            @PathVariable("orderId") Long orderId,
            @RequestParam("status") String newStatus,
            RedirectAttributes redirectAttributes) {

        Order order = orderService.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        order.setOrderStatus(newStatus);
        orderService.save(order);

        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái thành công!");
        return "redirect:/admin-orderdetail/" + orderId;
    }

    @PostMapping("/admin-order/process-return/{orderId}")
    public String processReturnRequest(
            @PathVariable("orderId") Long orderId,
            @RequestParam("returnDecision") String decision,
            @RequestParam(value = "adminResponse", required = false) String adminResponse,
            RedirectAttributes redirectAttributes) {

        Order order = orderService.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if ("ACCEPT".equals(decision)) {
            order.setReturnStatus("Chấp nhận trả hàng");
            order.setRefundStatus("Chờ hoàn tiền");
            order.setRefundAmount(order.getTotalPrice()); // Tự động lấy tổng tiền đơn hàng
            order.setReturnProcessedDate(LocalDateTime.now());
            order.setAdminResponse(adminResponse != null ? adminResponse : "Yêu cầu trả hàng đã được chấp nhận");
        } else {
            order.setReturnStatus("Từ chối");
            order.setAdminResponse(adminResponse != null ? adminResponse : "Yêu cầu trả hàng bị từ chối");
            order.setReturnProcessedDate(LocalDateTime.now());
        }

        orderService.save(order);

        redirectAttributes.addFlashAttribute("successMessage", "Đã xử lý yêu cầu trả hàng thành công!");
        return "redirect:/admin-orderdetail/" + orderId;
    }

    @PostMapping("/admin-order/mark-refund-completed/{orderId}")
    public String markRefundCompleted(
            @PathVariable("orderId") Long orderId,
            RedirectAttributes redirectAttributes) {

        Order order = orderService.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        order.setPaymentStatus("Đã hoàn tiền");
        order.setOrderStatus("Trả hàng thành công");
        orderService.save(order);

        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái hoàn tiền!");
        return "redirect:/admin-orderdetail/" + orderId;
    }

    @PostMapping("/admin-order/cancel/{orderId}")
    public String cancelOrder(
            @PathVariable("orderId") Long orderId,
            @RequestParam("cancelReason") String cancelReason,
            RedirectAttributes redirectAttributes) {

        Order order = orderService.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        order.setOrderStatus("Đã hủy");
        order.setCancelReason(cancelReason);
        order.setCancelDate(LocalDateTime.now());
        orderService.save(order);

        redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đơn hàng thành công!");
        return "redirect:/admin-orderdetail/" + orderId;
    }

}