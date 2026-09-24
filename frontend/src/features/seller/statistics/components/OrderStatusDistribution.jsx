import {
  getPercentage,
  ORDER_STATUSES,
  ORDER_STATUS_LABELS,
  formatNumber,
} from "../utils/statisticsUtils";
import styles from "../pages/SellerStatisticsPage.module.css";

function OrderStatusDistribution({ distribution = [] }) {
  const counts = Object.fromEntries(distribution.map((item) => [item.status, Number(item.count) || 0]));
  const total = ORDER_STATUSES.reduce((sum, status) => sum + (counts[status] || 0), 0);

  return (
    <div className={styles.progressList}>
      {ORDER_STATUSES.map((status) => {
        const count = counts[status] || 0;
        return (
          <div key={status} className={styles.progressItem}>
            <div><span>{ORDER_STATUS_LABELS[status]}</span><strong>{formatNumber(count)}</strong></div>
            <div className={styles.progressTrack}><span style={{ width: `${getPercentage(count, total)}%` }} /></div>
          </div>
        );
      })}
    </div>
  );
}

export default OrderStatusDistribution;
