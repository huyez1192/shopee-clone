package com.ecommerce.minishopee.statistics.service;

import com.ecommerce.minishopee.common.exception.BadRequestException;
import com.ecommerce.minishopee.order.repository.OrderItemRepository;
import com.ecommerce.minishopee.order.repository.OrderRepository;
import com.ecommerce.minishopee.review.repository.ReviewRepository;
import com.ecommerce.minishopee.shop.entity.Shop;
import com.ecommerce.minishopee.shop.enums.ShopStatus;
import com.ecommerce.minishopee.shop.exception.ShopNotFoundException;
import com.ecommerce.minishopee.shop.repository.ShopRepository;
import com.ecommerce.minishopee.statistics.dto.*;
import com.ecommerce.minishopee.statistics.enums.StatisticsGranularity;
import com.ecommerce.minishopee.statistics.projection.OrderFinancialSummaryProjection;
import com.ecommerce.minishopee.statistics.projection.OrderPeriodProjection;
import com.ecommerce.minishopee.statistics.projection.UnitsPeriodProjection;
import com.ecommerce.minishopee.user.entity.AppUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
public class SellerStatisticsService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReviewRepository reviewRepository;
    private final ShopRepository shopRepository;

    public SellerStatisticsService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                                   ReviewRepository reviewRepository, ShopRepository shopRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.reviewRepository = reviewRepository;
        this.shopRepository = shopRepository;
    }

    @Transactional(readOnly = true)
    public SellerStatisticsOverviewResponse getOverview(AppUser user, LocalDate from, LocalDate to) {
        Shop shop = approvedShop(user);
        StatisticsDateRange range = StatisticsDateRange.of(from, to);
        OrderFinancialSummaryProjection money = orderRepository.summarizeCompletedOrders(
                shop.getId(), range.fromInclusive(), range.toExclusive());
        return new SellerStatisticsOverviewResponse(from, to,
                money(money.getCompletedRevenue()), money(money.getCompletedGmv()),
                orderRepository.countCreatedOrders(shop.getId(), range.fromInclusive(), range.toExclusive()),
                value(money.getCompletedOrderCount()),
                orderItemRepository.sumUnitsSold(shop.getId(), range.fromInclusive(), range.toExclusive()),
                StatisticsMapper.statusDistribution(orderRepository.countByCurrentStatus(
                        shop.getId(), range.fromInclusive(), range.toExclusive())),
                StatisticsMapper.rating(reviewRepository.summarizeShopRatings(shop.getId())));
    }

    @Transactional(readOnly = true)
    public List<RevenueSeriesItemResponse> getRevenueSeries(AppUser user, LocalDate from, LocalDate to,
                                                            StatisticsGranularity granularity) {
        if (granularity == null) {
            throw new BadRequestException("granularity là bắt buộc.");
        }
        Shop shop = approvedShop(user);
        StatisticsDateRange range = StatisticsDateRange.of(from, to);
        List<OrderPeriodProjection> orders = granularity == StatisticsGranularity.DAY
                ? orderRepository.summarizeCompletedOrdersByDay(shop.getId(), range.fromInclusive(), range.toExclusive())
                : orderRepository.summarizeCompletedOrdersByMonth(shop.getId(), range.fromInclusive(), range.toExclusive());
        List<UnitsPeriodProjection> units = granularity == StatisticsGranularity.DAY
                ? orderItemRepository.sumUnitsSoldByDay(shop.getId(), range.fromInclusive(), range.toExclusive())
                : orderItemRepository.sumUnitsSoldByMonth(shop.getId(), range.fromInclusive(), range.toExclusive());

        Map<String, OrderPeriodProjection> orderByPeriod = new HashMap<>();
        orders.forEach(row -> orderByPeriod.put(key(row.getPeriodYear(), row.getPeriodMonth(), row.getPeriodDay(), granularity), row));
        Map<String, Long> unitsByPeriod = new HashMap<>();
        units.forEach(row -> unitsByPeriod.put(key(row.getPeriodYear(), row.getPeriodMonth(), row.getPeriodDay(), granularity), value(row.getUnitsSold())));

        List<RevenueSeriesItemResponse> result = new ArrayList<>();
        if (granularity == StatisticsGranularity.DAY) {
            for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
                addPeriod(result, date.toString(), orderByPeriod, unitsByPeriod);
            }
        } else {
            for (YearMonth month = YearMonth.from(from); !month.isAfter(YearMonth.from(to)); month = month.plusMonths(1)) {
                addPeriod(result, month.toString(), orderByPeriod, unitsByPeriod);
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<TopProductResponse> getTopProducts(AppUser user, LocalDate from, LocalDate to, int limit) {
        if (limit < 1 || limit > 20) {
            throw new BadRequestException("limit phải từ 1 đến 20.");
        }
        Shop shop = approvedShop(user);
        StatisticsDateRange range = StatisticsDateRange.of(from, to);
        return orderItemRepository.findTopProducts(shop.getId(), range.fromInclusive(), range.toExclusive(),
                        PageRequest.of(0, limit)).stream()
                .map(row -> new TopProductResponse(row.getProductId(), row.getProductName(), row.getImageUrl(),
                        value(row.getUnitsSold()), money(row.getMerchandiseValue())))
                .toList();
    }

    private Shop approvedShop(AppUser user) {
        Shop shop = shopRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new ShopNotFoundException("Bạn chưa có shop."));
        if (shop.getStatus() != ShopStatus.APPROVED) {
            throw new BadRequestException("Shop chưa được duyệt hoặc hiện không hoạt động.");
        }
        return shop;
    }

    private void addPeriod(List<RevenueSeriesItemResponse> result, String period,
                           Map<String, OrderPeriodProjection> orders, Map<String, Long> units) {
        OrderPeriodProjection row = orders.get(period);
        result.add(new RevenueSeriesItemResponse(period,
                row == null ? BigDecimal.ZERO : money(row.getCompletedRevenue()),
                row == null ? BigDecimal.ZERO : money(row.getCompletedGmv()),
                row == null ? 0L : value(row.getCompletedOrderCount()), units.getOrDefault(period, 0L)));
    }

    private String key(Integer year, Integer month, Integer day, StatisticsGranularity granularity) {
        return granularity == StatisticsGranularity.DAY
                ? LocalDate.of(year, month, day).toString()
                : YearMonth.of(year, month).toString();
    }

    private static BigDecimal money(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private static long value(Long value) { return value == null ? 0L : value; }
}
