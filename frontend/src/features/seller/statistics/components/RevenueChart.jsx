import { formatCurrency } from "../utils/statisticsUtils";
import styles from "../pages/SellerStatisticsPage.module.css";

const WIDTH = 900;
const HEIGHT = 280;
const PADDING = { top: 24, right: 24, bottom: 42, left: 70 };

function RevenueChart({ series }) {
  const values = series.map((item) => Number(item.completedRevenue) || 0);
  const maxValue = Math.max(...values, 0);
  const plotWidth = WIDTH - PADDING.left - PADDING.right;
  const plotHeight = HEIGHT - PADDING.top - PADDING.bottom;
  const denominator = Math.max(series.length - 1, 1);
  const points = series.map((item, index) => ({
    ...item,
    value: values[index],
    x: PADDING.left + (index / denominator) * plotWidth,
    y: PADDING.top + plotHeight - (maxValue ? (values[index] / maxValue) * plotHeight : 0),
  }));
  const labelStep = Math.max(1, Math.ceil(series.length / 7));

  if (series.length === 0 || maxValue === 0) {
    return <div className={styles.chartEmpty}>Chưa có doanh thu trong khoảng thời gian này.</div>;
  }

  return (
    <div className={styles.chartScroller}>
      <svg
        className={styles.chart}
        viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
        role="img"
        aria-label="Biểu đồ doanh thu hoàn tất theo thời gian"
      >
        {[0, 0.5, 1].map((ratio) => {
          const y = PADDING.top + plotHeight * ratio;
          const value = maxValue * (1 - ratio);
          return (
            <g key={ratio}>
              <line x1={PADDING.left} x2={WIDTH - PADDING.right} y1={y} y2={y} className={styles.gridLine} />
              <text x={PADDING.left - 10} y={y + 4} textAnchor="end" className={styles.axisText}>
                {new Intl.NumberFormat("vi-VN", { notation: "compact", maximumFractionDigits: 1 }).format(value)}
              </text>
            </g>
          );
        })}
        <polyline
          points={points.map((point) => `${point.x},${point.y}`).join(" ")}
          className={styles.revenueLine}
        />
        {points.map((point, index) => (
          <g key={point.period}>
            <circle cx={point.x} cy={point.y} r="4" className={styles.chartPoint}>
              <title>{`${point.period}: ${formatCurrency(point.value)}`}</title>
            </circle>
            {(index % labelStep === 0 || index === points.length - 1) && (
              <text x={point.x} y={HEIGHT - 15} textAnchor="middle" className={styles.axisText}>
                {point.period.slice(5)}
              </text>
            )}
          </g>
        ))}
      </svg>
    </div>
  );
}

export default RevenueChart;
