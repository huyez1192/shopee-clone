package com.ecommerce.minishopee.statistics.controller;

import com.ecommerce.minishopee.statistics.dto.AdminStatisticsOverviewResponse;
import com.ecommerce.minishopee.statistics.service.AdminStatisticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/statistics")
public class AdminStatisticsController {
    private final AdminStatisticsService statisticsService;

    public AdminStatisticsController(AdminStatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/overview")
    public AdminStatisticsOverviewResponse overview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return statisticsService.getOverview(from, to);
    }
}
