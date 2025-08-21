package com.datn.datn.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

public interface StatisticsService {

    public List<Map<String, Object>> getAllWeeksWithData(); // Lấy tất cả các tuần có dữ liệu

    List<Map<String, Object>> getMonthlyStatisticsByDateRange(LocalDateTime startDate, LocalDateTime endDate); // Lấy thống kê hàng tháng theo khoảng thời gian

    public List<Map<String, Object>> getDailyStatisticsByWeek(int year, int weekNumber); // Lấy thống kê hàng ngày theo tuần
    
    Map<String, Object> getDashboardStatistics();
    
    Map<String, Object> getRevenueAndOrdersStatistics();
    
    Map<String, Object> getCustomerStatistics();
    
    Map<String, Object> getProductStatistics();
}
