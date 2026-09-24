package com.ecommerce.minishopee.statistics.service;

import com.ecommerce.minishopee.common.exception.BadRequestException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

record StatisticsDateRange(LocalDate from, LocalDate to, LocalDateTime fromInclusive,
                           LocalDateTime toExclusive) {
    static StatisticsDateRange of(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BadRequestException("from và to là bắt buộc.");
        }
        if (from.isAfter(to)) {
            throw new BadRequestException("from không được sau to.");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > 366) {
            throw new BadRequestException("Khoảng thời gian không được vượt quá 366 ngày.");
        }
        return new StatisticsDateRange(from, to, from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }
}
