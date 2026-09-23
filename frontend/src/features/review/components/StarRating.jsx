import styles from "./StarRating.module.css";

function StarRating({ value, interactive = false, onChange, label = "Đánh giá" }) {
  return (
    <div className={styles.stars} aria-label={`${label}: ${value} trên 5 sao`}>
      {[1, 2, 3, 4, 5].map((star) => interactive ? (
        <button key={star} type="button" className={star <= value ? styles.filled : styles.empty}
          onClick={() => onChange(star)} aria-label={`${star} sao`}>★</button>
      ) : (
        <span key={star} className={star <= value ? styles.filled : styles.empty}>★</span>
      ))}
    </div>
  );
}

export default StarRating;
