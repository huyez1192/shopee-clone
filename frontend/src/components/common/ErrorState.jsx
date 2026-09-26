import styles from "./State.module.css";

function ErrorState({
  message = "Đã xảy ra lỗi.",
  onRetry,
}) {
  return (
    <div className={`${styles.state} ${styles.error}`} role="alert">
      <p>{message}</p>

      {onRetry && (
        <button
          type="button"
          className={styles.retry}
          onClick={onRetry}
        >
          Thử lại
        </button>
      )}
    </div>
  );
}

export default ErrorState;
