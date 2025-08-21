package com.datn.datn.service.impl;

import com.datn.datn.repository.MemberRepository;
import com.datn.datn.repository.OrderRepository;
import com.datn.datn.repository.ProductRepository;
import com.datn.datn.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Optional;
import com.datn.datn.service.*;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MemberRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override
    public List<Map<String, Object>> getMonthlyStatisticsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            List<Object[]> results = orderRepository.getMonthlyStatisticsByDateRange(startDate, endDate);
            List<Map<String, Object>> list = new ArrayList<>();

            for (Object[] row : results) {
                Map<String, Object> map = new HashMap<>();
                map.put("year", row[0]);
                map.put("month", row[1]);
                map.put("revenue", row[2] != null ? row[2] : 0);
                map.put("totalOrders", row[3] != null ? row[3] : 0);
                list.add(map);
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public List<Map<String, Object>> getAllWeeksWithData() {
        List<Object[]> results = orderRepository.getAllWeeksWithData();
        List<Map<String, Object>> list = new ArrayList<>();

        for (Object[] row : results) {
            Map<String, Object> map = new HashMap<>();
            map.put("weekNumber", row[0]);
            map.put("year", row[1]);
            list.add(map);
        }
        return list;
    }

    @Override
    public List<Map<String, Object>> getDailyStatisticsByWeek(int year, int weekNumber) {
        try {
            List<Object[]> results = orderRepository.getDailyStatisticsByWeek(year, weekNumber);
            List<Map<String, Object>> list = new ArrayList<>();

            for (Object[] row : results) {
                Map<String, Object> map = new HashMap<>();
                map.put("dayOfWeek", row[0]);
                map.put("dayName", row[1]);
                map.put("orderDate", row[2]);
                map.put("revenue", row[3] != null ? row[3] : 0);
                map.put("totalOrders", row[4] != null ? row[4] : 0);
                list.add(map);
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // @Override
    // public Map<String, Object> getDashboardStatistics() {
    // Map<String, Object> result = new HashMap<>();

    // result.putAll(getCustomerStatistics());
    // result.putAll(getProductStatistics());
    // return result;
    // }

    @Override
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> result = new HashMap<>();

        try {
            // Lấy dữ liệu tổng từ SQL Server
            BigDecimal totalRevenue = orderRepository.getTotalRevenue();
            Long totalOrders = orderRepository.getTotalOrders();

            // Log để kiểm tra
            System.out.println("Total Revenue: " + totalRevenue);
            System.out.println("Total Orders: " + totalOrders);

            // Chuyển đổi BigDecimal thành double để dễ xử lý trong JSON
            result.put("totalRevenue", totalRevenue != null ? totalRevenue.doubleValue() : 0.0);
            result.put("totalOrders", totalOrders != null ? totalOrders : 0L);

            // Tính phần trăm thay đổi
            result.put("revenueChangePercent", 0.0);
            result.put("ordersChangePercent", 0.0);

            // Thêm dữ liệu khách hàng và sản phẩm
            result.putAll(getCustomerStatistics());
            result.putAll(getProductStatistics());

            result.put("success", true);

        } catch (Exception e) {
            result.put("success", false);
            result.put("errorMessage", "Lỗi khi lấy dữ liệu thống kê: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> getCustomerStatistics() {
        Map<String, Object> result = new HashMap<>();
        try {
            long totalCustomers = customerRepository.countTotalCustomers();
            long newCustomers = customerRepository.countTotalCustomers(); // Dùng cùng giá trị

            result.put("totalCustomers", totalCustomers);
            result.put("newCustomers", newCustomers);

            // Vì không có dữ liệu để so sánh, set changePercent = 0
            result.put("customersChangePercent", 0.0);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("totalCustomers", 0L);
            result.put("newCustomers", 0L);
            result.put("customersChangePercent", 0.0);
        }
        return result;
    }

    @Override
    public Map<String, Object> getProductStatistics() {
        Map<String, Object> result = new HashMap<>();
        try {
            long totalProducts = productRepository.countActiveProducts();

            result.put("totalProducts", totalProducts);
            // Không cần newProducts và productsChangePercent nữa
            result.put("productsChangePercent", 0.0); // Set mặc định là 0%

        } catch (Exception e) {
            e.printStackTrace();
            result.put("totalProducts", 0L);
            result.put("productsChangePercent", 0.0);
        }
        return result;
    }
}
