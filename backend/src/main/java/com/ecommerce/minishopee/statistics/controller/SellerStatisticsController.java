package com.ecommerce.minishopee.statistics.controller;

import com.ecommerce.minishopee.statistics.dto.RevenueSeriesItemResponse;
import com.ecommerce.minishopee.statistics.dto.SellerStatisticsOverviewResponse;
import com.ecommerce.minishopee.statistics.dto.TopProductResponse;
import com.ecommerce.minishopee.statistics.enums.StatisticsGranularity;
import com.ecommerce.minishopee.statistics.service.SellerStatisticsService;
import com.ecommerce.minishopee.user.entity.AppUser;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/seller/statistics")
public class SellerStatisticsController {
    private final SellerStatisticsService statisticsService;

    public SellerStatisticsController(SellerStatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/overview")
    public SellerStatisticsOverviewResponse overview(
            @AuthenticationPrincipal AppUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return statisticsService.getOverview(user, from, to);
    }

    @GetMapping("/revenue-series")
    public List<RevenueSeriesItemResponse> revenueSeries(
            @AuthenticationPrincipal AppUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam StatisticsGranularity granularity) {
        return statisticsService.getRevenueSeries(user, from, to, granularity);
    }

    @GetMapping("/top-products")
    public List<TopProductResponse> topProducts(
            @AuthenticationPrincipal AppUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "5") int limit) {
        return statisticsService.getTopProducts(user, from, to, limit);
    }
}
