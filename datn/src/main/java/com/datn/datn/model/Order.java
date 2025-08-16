package com.datn.datn.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String orderCode; // Mã đơn hàng kiểu ORD20250808001

    // Khóa ngoại tới bảng members
    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @Column(name = "address", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String address;

    @Column(name = "note", columnDefinition = "NVARCHAR(500)")
    private String note;

    @Column(name = "total_price", nullable = false)
    private Double totalPrice;

    @Column(name = "payment_status", columnDefinition = "NVARCHAR(50)")
    private String paymentStatus;

    @Column(name = "order_status", columnDefinition = "NVARCHAR(50)")
    private String orderStatus;

    @Column(name = "payment_method", columnDefinition = "NVARCHAR(50)")
    private String paymentMethod;

    private Double discountAmount;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "cancel_date")
    private LocalDateTime cancelDate;

    @Column(name = "return_reason")
    private String returnReason;

    @Column(name = "return_request_date")
    private LocalDateTime returnRequestDate;

    @Column(name = "return_processed_date")
    private LocalDateTime returnProcessedDate;

    @Column(name = "return_status")
    private String returnStatus;

    @Column(name = "admin_response")
    private String adminResponse;

    @Column(name = "return_images")
    private String returnImages; // Lưu các URL ảnh cách nhau bởi dấu phẩy

    @Column(name = "return_method")
    private String returnMethod;

    @Column(name = "bank_account_number")
    private String bankAccountNumber;
    
    @Column(name = "bank_name")
    private String bankName;
    
    @Column(name = "account_holder")
    private String accountHolder;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderDetail> orderDetails = new ArrayList<>();

}
