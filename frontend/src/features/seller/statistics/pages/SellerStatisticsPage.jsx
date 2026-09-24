import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";

import EmptyState from "../../../../components/common/EmptyState";
import ErrorState from "../../../../components/common/ErrorState";
import LoadingState from "../../../../components/common/LoadingState";
import { queryKeys } from "../../../../query/queryKeys";
import { getApiErrorMessage } from "../../../../utils/getApiErrorMessage";
import {
  getSellerRevenueSeries,
  getSellerStatisticsOverview,
  getSellerTopProducts,
} from "../api/sellerStatisticsApi";
import OrderStatusDistribution from "../components/OrderStatusDistribution";
import RevenueChart from "../components/RevenueChart";
import ShopRatingSummary from "../components/ShopRatingSummary";
import StatisticsDateRange from "../components/StatisticsDateRange";
import StatisticsSummaryCards from "../components/StatisticsSummaryCards";
import TopProducts from "../components/TopProducts";
import { getDateRangePreset } from "../utils/statisticsUtils";

import styles from "./SellerStatisticsPage.module.css";

function Section({ title, subtitle, badge, children }) {
  return <section className={styles.section}>
    <header className={styles.sectionHeader}><div><h2>{title}</h2>{subtitle && <p>{subtitle}</p>}</div>{badge && <span className={styles.badge}>{badge}</span>}</header>
    {children}
  </section>;
}

function SellerStatisticsPage() {
  const [presetDays, setPresetDays] = useState(30);
  const range = useMemo(() => getDateRangePreset(presetDays), [presetDays]);
  const overviewParams = range;
  const revenueParams = { ...range, granularity: "DAY" };
  const topProductsParams = { ...range, limit: 5 };

  const overviewQuery = useQuery({
    queryKey: queryKeys.seller.statistics.overview(overviewParams),
    queryFn: () => getSellerStatisticsOverview(overviewParams),
  });
  const revenueQuery = useQuery({
    queryKey: queryKeys.seller.statistics.revenueSeries(revenueParams),
    queryFn: () => getSellerRevenueSeries(revenueParams),
  });
  const topProductsQuery = useQuery({
    queryKey: queryKeys.seller.statistics.topProducts(topProductsParams),
    queryFn: () => getSellerTopProducts(topProductsParams),
  });

  return (
    <div className={styles.page}>
      <header className={styles.pageHeader}>
        <div><h1>Tổng quan kinh doanh</h1><p>Theo dõi doanh thu, đơn hàng và hiệu quả bán hàng của shop.</p></div>
        <StatisticsDateRange value={presetDays} onChange={setPresetDays} />
      </header>

      {overviewQuery.isLoading ? <div className={styles.stateCard}><LoadingState message="Đang tải tổng quan..." /></div> : overviewQuery.error ? <div className={styles.stateCard}><ErrorState message={getApiErrorMessage(overviewQuery.error, "Không thể tải tổng quan kinh doanh.")} onRetry={() => overviewQuery.refetch()} /></div> : <>
        <StatisticsSummaryCards overview={overviewQuery.data} />
        <div className={styles.twoColumn}>
          <Section title="Trạng thái đơn hàng" subtitle="Trạng thái hiện tại của các đơn được tạo trong khoảng thời gian đã chọn.">
            <OrderStatusDistribution distribution={overviewQuery.data.orderStatusDistribution} />
          </Section>
          <Section title="Đánh giá shop" subtitle="Tổng hợp đánh giá của tất cả sản phẩm." badge="Toàn thời gian">
            <ShopRatingSummary rating={overviewQuery.data.ratingSummary} />
          </Section>
        </div>
      </>}

      <Section title="Doanh thu theo thời gian" subtitle="Doanh thu từ các đơn đã hoàn tất và thanh toán.">
        {revenueQuery.isLoading ? <LoadingState message="Đang tải biểu đồ doanh thu..." /> : revenueQuery.error ? <ErrorState message={getApiErrorMessage(revenueQuery.error, "Không thể tải dữ liệu doanh thu.")} onRetry={() => revenueQuery.refetch()} /> : <RevenueChart series={revenueQuery.data || []} />}
      </Section>

      <Section title="Sản phẩm bán chạy" subtitle="Xếp hạng theo số lượng bán trong khoảng thời gian đã chọn.">
        {topProductsQuery.isLoading ? <LoadingState message="Đang tải sản phẩm bán chạy..." /> : topProductsQuery.error ? <ErrorState message={getApiErrorMessage(topProductsQuery.error, "Không thể tải sản phẩm bán chạy.")} onRetry={() => topProductsQuery.refetch()} /> : (topProductsQuery.data || []).length === 0 ? <EmptyState title="Chưa có sản phẩm bán ra" description="Chưa có sản phẩm bán ra trong khoảng thời gian này." /> : <TopProducts products={topProductsQuery.data} />}
      </Section>
    </div>
  );
}

export default SellerStatisticsPage;
