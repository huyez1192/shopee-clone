import styles from "./State.module.css";

function LoadingState({
  message = "Đang tải dữ liệu...",
}) {
  return (
    <div className={`${styles.state} ${styles.loading}`} role="status" aria-live="polite">
      <p>{message}</p>
    </div>
  );
}

export default LoadingState;
