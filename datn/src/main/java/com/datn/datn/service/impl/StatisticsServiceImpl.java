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

    @Override
    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> result = new HashMap<>();
        result.putAll(getRevenueAndOrdersStatistics());
        result.putAll(getCustomerStatistics());
        result.putAll(getProductStatistics());
        return result;
    }

    @Override
    public Map<String, Object> getRevenueAndOrdersStatistics() {
        Map<String, Object> result = new HashMap<>();
        try {
            Object[] stats = orderRepository.getTotalRevenueAndOrders();

            System.out.println("Raw stats from DB: " + Arrays.toString(stats));
            System.out.println("Types: " +
                    (stats[0] != null ? stats[0].getClass().getSimpleName() : "null") + ", " +
                    (stats[1] != null ? stats[1].getClass().getSimpleName() : "null") + ", " +
                    (stats[2] != null ? stats[2].getClass().getSimpleName() : "null") + ", " +
                    (stats[3] != null ? stats[3].getClass().getSimpleName() : "null"));

            if (stats != null && stats.length >= 4) {
                // SQL Server trả về BigInteger cho SUM, Long cho COUNT
                BigInteger totalRevenueBigInt = (BigInteger) stats[0];
                Long totalOrders = (Long) stats[1];
                BigInteger lastMonthRevenueBigInt = (BigInteger) stats[2];
                Long lastMonthOrders = (Long) stats[3];

                // Convert BigInteger to BigDecimal
                BigDecimal totalRevenue = new BigDecimal(totalRevenueBigInt);
                BigDecimal lastMonthRevenue = new BigDecimal(lastMonthRevenueBigInt);

                System.out.println("Converted - Total Revenue: " + totalRevenue);
                System.out.println("Converted - Total Orders: " + totalOrders);

                result.put("totalRevenue", totalRevenue);
                result.put("totalOrders", totalOrders);
                result.put("lastMonthRevenue", lastMonthRevenue);
                result.put("lastMonthOrders", lastMonthOrders);

                // Tính phần trăm thay đổi
                if (lastMonthRevenue.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal revenueChange = totalRevenue.subtract(lastMonthRevenue)
                            .divide(lastMonthRevenue, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                    result.put("revenueChangePercent", revenueChange.doubleValue());
                    System.out.println("Revenue change percent: " + revenueChange + "%");
                } else {
                    result.put("revenueChangePercent", 0.0);
                    System.out.println("No last month revenue data");
                }

                if (lastMonthOrders > 0) {
                    double ordersChangePercent = ((totalOrders - lastMonthOrders) / (double) lastMonthOrders) * 100;
                    result.put("ordersChangePercent", ordersChangePercent);
                    System.out.println("Orders change percent: " + ordersChangePercent + "%");
                } else {
                    result.put("ordersChangePercent", 0.0);
                    System.out.println("No last month orders data");
                }
            }
        } catch (Exception e) {
            System.err.println("Error in getRevenueAndOrdersStatistics: " + e.getMessage());
            e.printStackTrace();
            result.put("totalRevenue", BigDecimal.ZERO);
            result.put("totalOrders", 0L);
            result.put("revenueChangePercent", 0.0);
            result.put("ordersChangePercent", 0.0);
        }
        return result;
    }

    // Helper methods để convert dữ liệu từ native query
    private BigDecimal convertToBigDecimal(Object value) {
        if (value == null)
            return BigDecimal.ZERO;
        if (value instanceof BigDecimal)
            return (BigDecimal) value;
        if (value instanceof Number)
            return BigDecimal.valueOf(((Number) value).doubleValue());
        if (value instanceof String)
            return new BigDecimal((String) value);
        return BigDecimal.ZERO;
    }

    private Long convertToLong(Object value) {
        if (value == null)
            return 0L;
        if (value instanceof Long)
            return (Long) value;
        if (value instanceof Number)
            return ((Number) value).longValue();
        if (value instanceof String)
            return Long.parseLong((String) value);
        return 0L;
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
