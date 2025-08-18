package com.datn.datn.service.impl;

import com.datn.datn.model.Member;
import com.datn.datn.model.Order;
import com.datn.datn.model.OrderDetail;
import com.datn.datn.model.ProductVariant;
import com.datn.datn.repository.OrderRepository;
import com.datn.datn.repository.ProductVariantRepository;
import com.datn.datn.service.OrderService;

import jakarta.persistence.EntityNotFoundException;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ProductVariantRepository repo; // Assuming you have a ProductVariantRepository to handle product variants

    @Override
    public Order save(Order order) {
        if (order.getOrderCode() == null || order.getOrderCode().isEmpty()) {
            order.setOrderCode(generateOrderCode());
        }
        return orderRepository.save(order);
    }

    private String generateOrderCode() {
        // Lấy ngày hiện tại dạng yyyyMMdd
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // Đếm số đơn đã tạo trong ngày
        long countToday = orderRepository.countByOrderDateBetween(
                LocalDate.now().atStartOfDay(),
                LocalDate.now().atTime(23, 59, 59));

        // Số thứ tự + 1 và format thành 3 chữ số
        String sequence = String.format("%03d", countToday + 1);

        return "ORD" + datePart + sequence;
    }

    @Override
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    @Override
    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    @Override
    public void deleteById(Long id) {
        orderRepository.deleteById(id);
    }

    @Override
    public List<Order> findByMemberAndOrderDateAfter(Member member, LocalDateTime date) {
        return orderRepository.findByMemberAndOrderDateAfter(member, date);
    }

    @Override
    public List<Order> findLatestOrdersByMember(Member member) {
        return orderRepository.findByMemberOrderByOrderDateDesc(member);
    }

    @Override
    public List<String> getMemberAddresses(Long memberId) {
        return orderRepository.findDistinctAddressesByMemberId(memberId);
    }

    @Override
    public List<Order> getOrdersByMemberId(Long memberId) {
        return orderRepository.findByMemberId(memberId);
    }

    @Override
    public void cancelOrder(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));

        if (!canBeCancelled(order.getOrderStatus())) {
            throw new IllegalStateException("Không thể hủy đơn hàng ở trạng thái: " + order.getOrderStatus());
        }

        order.setOrderStatus("Đã hủy");
        order.setCancelReason(reason);
        order.setCancelDate(LocalDateTime.now());

        restoreProductQuantities(order);

        // Xử lý hoàn tiền nếu đã thanh toán
        if ("Đã thanh toán".equals(order.getPaymentStatus())) {
            processRefund(order);
        }

        orderRepository.save(order);
        sendCancellationNotification(order);
    }

    private void processRefund(Order order) {
        try {
            // Gọi API hoàn tiền của cổng thanh toán
            // Ví dụ: paymentGatewayService.processRefund(order.getTransactionId(),
            // order.getTotalPrice());

            // Cập nhật trạng thái thanh toán
            order.setPaymentStatus("Đã hoàn tiền");
        } catch (Exception e) {
            // Log lỗi và có thể thông báo cho admin
            System.err.println("Lỗi khi hoàn tiền cho đơn hàng #" + order.getOrderCode() + ": " + e.getMessage());
            // Vẫn tiếp tục hủy đơn hàng nhưng đánh dấu cần xử lý hoàn tiền thủ công
            order.setPaymentStatus("Chờ hoàn tiền");
        }
    }

    private boolean canBeCancelled(String status) {
        return !status.equals("Đang vận chuyển")
                && !status.equals("Đã giao hàng")
                && !status.equals("Đã hủy");
    }

    private void restoreProductQuantities(Order order) {
        for (OrderDetail detail : order.getOrderDetails()) {
            ProductVariant variant = detail.getProductVariant();
            variant.setQuantityInStock(variant.getQuantityInStock() + detail.getQuantity());
            repo.save(variant);
        }
    }

    private void sendCancellationNotification(Order order) {
        try {
            String message = "Đơn hàng #" + order.getOrderCode() + " đã được hủy. Lý do: " + order.getCancelReason();
        } catch (Exception e) {
            // Log lỗi nhưng không làm gián đoạn quá trình hủy đơn hàng
            System.err.println("Không thể gửi thông báo: " + e.getMessage());
        }
    }

    @Override
    public void requestReturn(Long orderId, String reason, String returnMethod, List<String> imageUrls) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));

        // Lưu danh sách ảnh (cách nhau bởi dấu phẩy)
        if (imageUrls != null && !imageUrls.isEmpty()) {
            order.setReturnImages(String.join(",", imageUrls));
        }

        // Cập nhật thông tin trả hàng
        order.setOrderStatus("Yêu cầu trả hàng");
        order.setReturnStatus("Chờ xử lý");
        order.setReturnMethod(returnMethod);
        order.setReturnReason(reason);
        order.setReturnRequestDate(LocalDateTime.now());

        orderRepository.save(order);
    }

    @Override
    public String generateOrderId() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'generateOrderId'");
    }

    @Override
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));
    }

    @Override
    public Page<Order> getOrdersByMember(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable);
    }

    @Override
    public Page<Order> searchOrdersByMember(Long memberId, String keyword, Pageable pageable) {
        return orderRepository.findByMemberIdAndOrderCodeContainingOrAddressContaining(memberId, keyword, keyword,
                pageable);
    }
}