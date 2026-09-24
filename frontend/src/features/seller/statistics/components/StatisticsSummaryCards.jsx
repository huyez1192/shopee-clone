import { formatCurrency, formatNumber } from "../utils/statisticsUtils";
import styles from "../pages/SellerStatisticsPage.module.css";

function StatisticsSummaryCards({ overview }) {
  const cards = [
    ["Doanh thu hoàn tất", formatCurrency(overview.completedRevenue)],
    ["GMV hoàn tất", formatCurrency(overview.completedGmv)],
    ["Tổng đơn được tạo", formatNumber(overview.totalOrdersCreated)],
    ["Đơn hoàn tất", formatNumber(overview.completedOrderCount)],
    ["Sản phẩm đã bán", formatNumber(overview.unitsSold)],
  ];

  return (
    <div className={styles.summaryGrid}>
      {cards.map(([label, value]) => (
        <article key={label} className={styles.summaryCard}>
          <span>{label}</span>
          <strong>{value}</strong>
        </article>
      ))}
    </div>
  );
}

export default StatisticsSummaryCards;
