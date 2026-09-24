package com.ecommerce.minishopee.statistics.service;

import com.ecommerce.minishopee.common.exception.BadRequestException;
import com.ecommerce.minishopee.order.repository.OrderItemRepository;
import com.ecommerce.minishopee.order.repository.OrderRepository;
import com.ecommerce.minishopee.review.repository.ProductRatingSummaryProjection;
import com.ecommerce.minishopee.review.repository.ReviewRepository;
import com.ecommerce.minishopee.shop.entity.Shop;
import com.ecommerce.minishopee.shop.enums.ShopStatus;
import com.ecommerce.minishopee.shop.repository.ShopRepository;
import com.ecommerce.minishopee.statistics.enums.StatisticsGranularity;
import com.ecommerce.minishopee.statistics.projection.OrderFinancialSummaryProjection;
import com.ecommerce.minishopee.statistics.projection.OrderPeriodProjection;
import com.ecommerce.minishopee.statistics.projection.TopProductProjection;
import com.ecommerce.minishopee.statistics.projection.UnitsPeriodProjection;
import com.ecommerce.minishopee.user.entity.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SellerStatisticsServiceTests {
    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock ShopRepository shopRepository;
    @Mock OrderFinancialSummaryProjection financial;
    @Mock ProductRatingSummaryProjection rating;
    @Mock OrderPeriodProjection orderPeriod;
    @Mock UnitsPeriodProjection unitsPeriod;
    @Mock TopProductProjection topProduct;

    SellerStatisticsService service;
    AppUser seller;

    @BeforeEach
    void setUp() {
        service = new SellerStatisticsService(orderRepository, orderItemRepository, reviewRepository, shopRepository);
        seller = new AppUser(); seller.setId(7L);
        Shop shop = new Shop(); shop.setId(11L); shop.setOwner(seller); shop.setStatus(ShopStatus.APPROVED);
        when(shopRepository.findByOwnerId(7L)).thenReturn(Optional.of(shop));
    }

    @Test
    void overviewKeepsRevenueGmvOrdersUnitsAndAllTimeRatingSeparate() {
        when(orderRepository.summarizeCompletedOrders(eq(11L), any(), any())).thenReturn(financial);
        when(financial.getCompletedRevenue()).thenReturn(new BigDecimal("100.00"));
        when(financial.getCompletedGmv()).thenReturn(new BigDecimal("115.00"));
        when(financial.getCompletedOrderCount()).thenReturn(2L);
        when(orderRepository.countCreatedOrders(eq(11L), any(), any())).thenReturn(6L);
        when(orderRepository.countByCurrentStatus(eq(11L), any(), any())).thenReturn(List.of());
        when(orderItemRepository.sumUnitsSold(eq(11L), any(), any())).thenReturn(4L);
        when(reviewRepository.summarizeShopRatings(11L)).thenReturn(rating);
        when(rating.getAverageRating()).thenReturn(4.5);
        when(rating.getReviewCount()).thenReturn(2L);

        var response = service.getOverview(seller, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertEquals(new BigDecimal("100.00"), response.completedRevenue());
        assertEquals(new BigDecimal("115.00"), response.completedGmv());
        assertEquals(6, response.totalOrdersCreated());
        assertEquals(2, response.completedOrderCount());
        assertEquals(4, response.unitsSold());
        assertEquals(5, response.orderStatusDistribution().size());
        assertEquals(4.5, response.ratingSummary().averageRating());
        verify(reviewRepository).summarizeShopRatings(11L);
    }

    @Test
    void sameDayUsesHalfOpenWholeDayBounds() {
        stubOverview();
        service.getOverview(seller, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10));
        verify(orderRepository).summarizeCompletedOrders(11L,
                LocalDateTime.of(2026, 9, 10, 0, 0), LocalDateTime.of(2026, 9, 11, 0, 0));
    }

    @Test
    void invalidAndOverlongRangesAreRejected() {
        assertThrows(BadRequestException.class, () -> service.getOverview(seller,
                LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 1)));
        assertThrows(BadRequestException.class, () -> service.getOverview(seller,
                LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 2)));
    }

    @Test
    void daySeriesZeroFillsMissingPeriods() {
        when(orderRepository.summarizeCompletedOrdersByDay(eq(11L), any(), any())).thenReturn(List.of(orderPeriod));
        when(orderItemRepository.sumUnitsSoldByDay(eq(11L), any(), any())).thenReturn(List.of(unitsPeriod));
        when(orderPeriod.getPeriodYear()).thenReturn(2026); when(orderPeriod.getPeriodMonth()).thenReturn(9);
        when(orderPeriod.getPeriodDay()).thenReturn(2); when(orderPeriod.getCompletedRevenue()).thenReturn(BigDecimal.TEN);
        when(orderPeriod.getCompletedGmv()).thenReturn(new BigDecimal("12")); when(orderPeriod.getCompletedOrderCount()).thenReturn(1L);
        when(unitsPeriod.getPeriodYear()).thenReturn(2026); when(unitsPeriod.getPeriodMonth()).thenReturn(9);
        when(unitsPeriod.getPeriodDay()).thenReturn(2); when(unitsPeriod.getUnitsSold()).thenReturn(3L);

        var series = service.getRevenueSeries(seller, LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 3), StatisticsGranularity.DAY);

        assertEquals(3, series.size());
        assertEquals(BigDecimal.ZERO, series.getFirst().completedRevenue());
        assertEquals(BigDecimal.TEN, series.get(1).completedRevenue());
        assertEquals(3, series.get(1).unitsSold());
        assertEquals(BigDecimal.ZERO, series.getLast().completedGmv());
    }

    @Test
    void monthSeriesZeroFillsMissingMonths() {
        when(orderRepository.summarizeCompletedOrdersByMonth(eq(11L), any(), any())).thenReturn(List.of());
        when(orderItemRepository.sumUnitsSoldByMonth(eq(11L), any(), any())).thenReturn(List.of());
        var series = service.getRevenueSeries(seller, LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 3, 1), StatisticsGranularity.MONTH);
        assertEquals(List.of("2026-01", "2026-02", "2026-03"), series.stream().map(item -> item.period()).toList());
    }

    @Test
    void topProductReturnsSnapshotAggregateAndValidatesLimit() {
        when(orderItemRepository.findTopProducts(eq(11L), any(), any(), any())).thenReturn(List.of(topProduct));
        when(topProduct.getProductId()).thenReturn(9L); when(topProduct.getProductName()).thenReturn("Snapshot name");
        when(topProduct.getImageUrl()).thenReturn("https://example.com/snapshot.jpg");
        when(topProduct.getUnitsSold()).thenReturn(8L); when(topProduct.getMerchandiseValue()).thenReturn(new BigDecimal("240"));
        var result = service.getTopProducts(seller, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), 5);
        assertEquals("Snapshot name", result.getFirst().productName());
        assertEquals(new BigDecimal("240"), result.getFirst().merchandiseValue());
        assertThrows(BadRequestException.class, () -> service.getTopProducts(seller,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), 21));
    }

    private void stubOverview() {
        when(orderRepository.summarizeCompletedOrders(eq(11L), any(), any())).thenReturn(financial);
        when(orderRepository.countByCurrentStatus(eq(11L), any(), any())).thenReturn(List.of());
        when(reviewRepository.summarizeShopRatings(11L)).thenReturn(rating);
    }
}
