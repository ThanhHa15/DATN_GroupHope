package com.datn.datn.controller.admin;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.datn.datn.service.StatisticsService;

@Controller
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    @GetMapping("/statistics")
    public String statistics() {
        return "formStatisticts";
    }

    // biểu đồ tuần
    @GetMapping("/dailyByWeek")
    @ResponseBody
    public List<Map<String, Object>> getDailyStatisticsByWeek(
            @RequestParam("year") int year,
            @RequestParam("weekNumber") int weekNumber) {
        return statisticsService.getDailyStatisticsByWeek(year, weekNumber);
    }

    // lấy tuần theo năm
    @GetMapping("/allWeeks")
    @ResponseBody
    public List<Map<String, Object>> getAllWeeksWithData() {
        return statisticsService.getAllWeeksWithData();
    }

    // biểu đồ tháng
    @GetMapping("/monthlyByDate")
    @ResponseBody
    public List<Map<String, Object>> getMonthlyStatisticsByDateRange(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return statisticsService.getMonthlyStatisticsByDateRange(startDate, endDate);
    }

    @GetMapping("/dashboardd")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = statisticsService.getDashboardStats();

        Boolean success = (Boolean) stats.get("success");
        if (success != null && success) {
            return ResponseEntity.ok(stats);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(stats);
        }
    }

}
