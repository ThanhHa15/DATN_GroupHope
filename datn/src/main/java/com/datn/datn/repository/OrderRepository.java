package com.datn.datn.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.datn.datn.model.Member;
import com.datn.datn.model.Order;
import com.datn.datn.model.Vouchers;

public interface OrderRepository extends JpaRepository<Order, Long>,
                JpaSpecificationExecutor<Order> {

        Page<Order> findByMemberIdAndOrderCodeContainingOrAddressContaining(Long memberId, String code, String address,
                        Pageable pageable);

        // Lấy đơn hàng theo member, phân trang
        Page<Order> findByMemberId(Long memberId, Pageable pageable);

        // Tìm kiếm theo mã đơn hàng hoặc địa chỉ
        @Query("SELECT o FROM Order o WHERE o.member.id = :memberId AND " +
                        "(LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(o.address) LIKE LOWER(CONCAT('%', :keyword, '%')))")
        Page<Order> searchOrdersByMember(@Param("memberId") Long memberId, @Param("keyword") String keyword,
                        Pageable pageable);

        List<Order> findByMemberAndOrderDateAfter(Member member, LocalDateTime date);

        List<Order> findByMemberOrderByOrderDateDesc(Member member);

        @Query("SELECT DISTINCT o.address FROM Order o WHERE o.member.id = :memberId")
        List<String> findDistinctAddressesByMemberId(@Param("memberId") Long memberId);

        List<Order> findByMemberId(Long memberId);

        @Query("SELECT COUNT(o) FROM Order o WHERE DATE(o.orderDate) = :date")
        int countByOrderDate(@Param("date") LocalDate date);

        long countByOrderDateBetween(LocalDateTime start, LocalDateTime end);

        @Query("SELECT o FROM Order o WHERE o.id = :id AND o.member.id = :memberId")
        Optional<Order> findByIdAndMemberId(@Param("id") Long id, @Param("memberId") Long memberId);

        @Query("SELECT o FROM Order o WHERE o.member.id = :memberId AND o.orderStatus = 'Yêu cầu trả hàng'")
        List<Order> findReturnRequestsByMemberId(@Param("memberId") Long memberId);

        // Thêm các method mới trong OrderRepository
        @Query(value = """
                            SELECT
                                YEAR(o.order_date) AS year,
                                MONTH(o.order_date) AS month,
                                SUM(o.total_price - COALESCE(o.discount_amount, 0)) AS revenue,
                                COUNT(o.id) AS totalOrders
                            FROM orders o
                            WHERE o.payment_status = N'Đã thanh toán'
                                AND o.order_date BETWEEN :startDate AND :endDate
                            GROUP BY YEAR(o.order_date), MONTH(o.order_date)
                            ORDER BY year, month
                        """, nativeQuery = true)
        List<Object[]> getMonthlyStatisticsByDateRange(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        // Query cho tất cả các tuần có dữ liệu
        @Query(value = """
                            SELECT DISTINCT
                                DATEPART(ISO_WEEK, o.order_date) AS weekNumber,
                                YEAR(o.order_date) AS year
                            FROM orders o
                            WHERE o.payment_status = N'Đã thanh toán'
                            ORDER BY year, weekNumber
                        """, nativeQuery = true)
        List<Object[]> getAllWeeksWithData();

        // Query cho doanh thu theo ngày trong một tuần cụ thể
        @Query(value = """
                            SELECT
                                DATEPART(WEEKDAY, o.order_date) AS dayOfWeek,
                                DATENAME(WEEKDAY, o.order_date) AS dayName,
                                CAST(o.order_date AS DATE) AS orderDate,
                                SUM(o.total_price - COALESCE(o.discount_amount, 0)) AS revenue,
                                COUNT(o.id) AS totalOrders
                            FROM orders o
                            WHERE o.payment_status = N'Đã thanh toán'
                                AND DATEPART(ISO_WEEK, o.order_date) = :weekNumber
                                AND YEAR(o.order_date) = :year
                            GROUP BY DATEPART(WEEKDAY, o.order_date), DATENAME(WEEKDAY, o.order_date), CAST(o.order_date AS DATE)
                            ORDER BY dayOfWeek
                        """, nativeQuery = true)
        List<Object[]> getDailyStatisticsByWeek(@Param("year") int year, @Param("weekNumber") int weekNumber);

        @Query(value = "SELECT ISNULL(SUM(total_price), 0) FROM orders " +
                        "WHERE payment_status = N'Đã thanh toán' AND order_status = N'Đã giao hàng'", nativeQuery = true)
        BigDecimal getTotalRevenue();

        @Query(value = "SELECT COUNT(*) FROM orders " +
                        "WHERE payment_status = N'Đã thanh toán' AND order_status = N'Đã giao hàng'", nativeQuery = true)
        Long getTotalOrders();

        List<Order> findByOrderDateBetween(LocalDateTime start, LocalDateTime end);

        List<Order> findByOrderDateAfter(LocalDateTime start);

        List<Order> findByOrderDateBefore(LocalDateTime end);
}
