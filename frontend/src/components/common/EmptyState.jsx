import styles from "./State.module.css";

function EmptyState({
  title = "Không có dữ liệu",
  description,
}) {
  return (
    <div className={styles.state}>
      <h2>{title}</h2>

      {description && (
        <p>{description}</p>
      )}
    </div>
  );
}

export default EmptyState;
