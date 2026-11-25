package com.community.controller.api;

import com.community.common.ApiResponse;
import com.community.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理端统计接口：统一响应格式，供看板图表异步刷新
 */
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class StatsApiController {

    private final StatisticsService statisticsService;

    @GetMapping("/overview")
    public ApiResponse<StatisticsService.Overview> overview() {
        return ApiResponse.ok(statisticsService.overview());
    }

    @GetMapping("/trend")
    public ApiResponse<?> trend(@RequestParam(defaultValue = "7") int days) {
        return ApiResponse.ok(statisticsService.trend(Math.min(Math.max(days, 3), 30)));
    }

    @GetMapping("/category")
    public ApiResponse<?> category() {
        return ApiResponse.ok(statisticsService.categoryShare());
    }

    @GetMapping("/status")
    public ApiResponse<?> status() {
        return ApiResponse.ok(statisticsService.statusDistribution());
    }

    @GetMapping("/staff")
    public ApiResponse<?> staff() {
        return ApiResponse.ok(statisticsService.staffPerformance());
    }

    @GetMapping("/all")
    public ApiResponse<Map<String, Object>> all() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("overview", statisticsService.overview());
        data.put("trend", statisticsService.trend(7));
        data.put("category", statisticsService.categoryShare());
        data.put("status", statisticsService.statusDistribution());
        data.put("staff", statisticsService.staffPerformance());
        return ApiResponse.ok(data);
    }
}
