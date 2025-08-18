package com.datn.datn.controller.user;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.datn.datn.model.Member;
import com.datn.datn.model.Order;
import com.datn.datn.model.Product;
import com.datn.datn.repository.OrderDetailRepository;
import com.datn.datn.repository.OrderRepository;
import com.datn.datn.repository.ProductVariantRepository;
import com.datn.datn.service.OrderService;
import com.datn.datn.service.ProductService;

import jakarta.servlet.http.HttpSession;

@Controller
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderDetailRepository orderDetailRepository;
    @Autowired
    private ProductVariantRepository productVariantRepository;

    @GetMapping("/order")
    public String getOrders(Model model, HttpSession session) {
        Member currentUser = (Member) session.getAttribute("loggedInUser");
        if (currentUser != null) {
            List<Order> orders = orderService.getOrdersByMemberId(currentUser.getId());
            orders.sort(Comparator.comparing(Order::getId).reversed());

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

    @PostMapping("/order/cancel/{orderId}")
    public ResponseEntity<?> cancelOrder(
            @PathVariable("orderId") Long orderId,
            @RequestBody Map<String, String> requestBody,
            HttpSession session) {

        Member member = (Member) session.getAttribute("loggedInUser");
        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("success", false, "message", "Vui lòng đăng nhập"));
        }

        Order order = orderService.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // Kiểm tra quyền
        if (!order.getMember().getId().equals(member.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    Map.of("success", false, "message", "Bạn không có quyền hủy đơn hàng này"));
        }

        // Kiểm tra trạng thái đơn hàng
        if (order.getOrderStatus().equals("Đã giao hàng") ||
                order.getOrderStatus().equals("Đang vận chuyển") ||
                order.getOrderStatus().equals("Đã hủy")) {
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", "Không thể hủy đơn hàng ở trạng thái hiện tại"));
        }

        try {
            String reason = requestBody.get("reason");
            orderService.cancelOrder(orderId, reason);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping(value = "/order/return/{orderId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> returnOrder(
            @PathVariable("orderId") Long orderId,
            @RequestParam("reason") String reason,
            @RequestParam("returnMethod") String returnMethod,
            @RequestParam String bankAccountNumber,
            @RequestParam String bankName,
            @RequestParam String accountHolder,
            @RequestParam("imageFile") MultipartFile imageFiles,
            HttpSession session) {

        try {
            Member member = (Member) session.getAttribute("loggedInUser");
            if (member == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        Map.of("success", false, "message", "Vui lòng đăng nhập"));
            }

            Order order = orderService.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
            order.setReturnReason(reason);
            order.setReturnMethod(returnMethod);
            order.setBankAccountNumber(bankAccountNumber);
            order.setBankName(bankName);
            order.setAccountHolder(accountHolder);
            // order.setReturnStatus("Chờ xử lý");
            order.setReturnRequestDate(LocalDateTime.now());

            if (!order.getMember().getId().equals(member.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        Map.of("success", false, "message", "Bạn không có quyền trả hàng đơn hàng này"));
            }

            if (!order.getOrderStatus().equals("Đã giao hàng") ||
                    !order.getPaymentStatus().equals("Đã thanh toán")) {
                return ResponseEntity.badRequest().body(
                        Map.of("success", false, "message",
                                "Chỉ có thể trả hàng khi đơn hàng đã được giao và thanh toán"));
            }

            List<String> imageUrls = new ArrayList<>();
            String uploadDir = new File("src/main/resources/static/images").getAbsolutePath();
            File uploadPath = new File(uploadDir);

            if (!imageFiles.isEmpty()) {
                try {
                    // Tạo tên file duy nhất để tránh trùng lặp
                    String originalFileName = imageFiles.getOriginalFilename();
                    String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
                    String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

                    // Lưu file
                    File dest = new File(uploadPath, uniqueFileName);
                    imageFiles.transferTo(dest);

                    // Lưu đường dẫn ảnh
                    String imageUrl = "/images/" + uniqueFileName;
                    imageUrls.add(imageUrl);
                    System.out.println("Đã lưu ảnh thành công: " + imageUrl);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Lỗi khi lưu ảnh: " + e.getMessage());
                }

            }

            orderService.requestReturn(orderId, reason, returnMethod, imageUrls);

            return ResponseEntity.ok(Map.of("success", true));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    Map.of("success", false, "message", e.getMessage()));
        }
    }

}