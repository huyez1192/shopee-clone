package com.ecommerce.minishopee.statistics.service;

import com.ecommerce.minishopee.order.repository.OrderItemRepository;
import com.ecommerce.minishopee.order.repository.OrderRepository;
import com.ecommerce.minishopee.product.repository.ProductRepository;
import com.ecommerce.minishopee.shop.repository.ShopRepository;
import com.ecommerce.minishopee.statistics.dto.AdminStatisticsOverviewResponse;
import com.ecommerce.minishopee.statistics.projection.OrderFinancialSummaryProjection;
import com.ecommerce.minishopee.user.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class AdminStatisticsService {
    private final AppUserRepository userRepository;
    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public AdminStatisticsService(AppUserRepository userRepository, ShopRepository shopRepository,
                                  ProductRepository productRepository, OrderRepository orderRepository,
                                  OrderItemRepository orderItemRepository) {
        this.userRepository = userRepository;
        this.shopRepository = shopRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional(readOnly = true)
    public AdminStatisticsOverviewResponse getOverview(LocalDate from, LocalDate to) {
        StatisticsDateRange range = StatisticsDateRange.of(from, to);
        OrderFinancialSummaryProjection summary = orderRepository.summarizeCompletedOrders(
                null, range.fromInclusive(), range.toExclusive());
        return new AdminStatisticsOverviewResponse(from, to,
                userRepository.count(), shopRepository.count(), productRepository.count(), orderRepository.count(),
                value(summary.getCompletedOrderCount()), money(summary.getCompletedGmv()),
                money(summary.getCompletedRevenue()),
                orderItemRepository.sumUnitsSold(null, range.fromInclusive(), range.toExclusive()),
                StatisticsMapper.statusDistribution(orderRepository.countByCurrentStatus(
                        null, range.fromInclusive(), range.toExclusive())));
    }

    private static BigDecimal money(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private static long value(Long value) { return value == null ? 0L : value; }
}
