package com.datn.datn.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.datn.datn.model.Order;
import com.datn.datn.model.OrderDetail;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    List<OrderDetail> findByOrder(Order order);

     @Query("SELECT od.productVariant.variantID FROM OrderDetail od WHERE od.order.id = :orderId")
    Integer findVariantIdByOrderId(@Param("orderId") Long orderId);

    // Hoặc nếu bạn muốn lấy cả đối tượng OrderDetail
    OrderDetail findByOrderId(Long orderId);
}