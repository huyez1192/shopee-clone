import styles from "../pages/SellerStatisticsPage.module.css";

const PRESETS = [7, 30, 90];

function StatisticsDateRange({ value, onChange }) {
  return (
    <div className={styles.datePresets} aria-label="Khoảng thời gian thống kê">
      {PRESETS.map((days) => (
        <button
          key={days}
          type="button"
          className={value === days ? styles.activePreset : styles.preset}
          onClick={() => onChange(days)}
          aria-pressed={value === days}
        >
          {days} ngày
        </button>
      ))}
    </div>
  );
}

export default StatisticsDateRange;
