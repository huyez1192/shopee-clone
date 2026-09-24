import { formatNumber, getPercentage } from "../utils/statisticsUtils";
import styles from "../pages/SellerStatisticsPage.module.css";

const STARS = [5, 4, 3, 2, 1];
const COUNT_FIELDS = { 5: "fiveStarCount", 4: "fourStarCount", 3: "threeStarCount", 2: "twoStarCount", 1: "oneStarCount" };

function ShopRatingSummary({ rating }) {
  const reviewCount = Number(rating?.reviewCount) || 0;
  const average = Number(rating?.averageRating) || 0;
  return (
    <div>
      <div className={styles.ratingHeadline}>
        <strong>{average.toFixed(1)} <small>/ 5</small></strong>
        <span>{formatNumber(reviewCount)} đánh giá</span>
      </div>
      <div className={styles.ratingRows}>
        {STARS.map((star) => {
          const count = Number(rating?.[COUNT_FIELDS[star]]) || 0;
          return <div key={star} className={styles.ratingRow}>
            <span>{star} sao</span>
            <div className={styles.progressTrack}><span style={{ width: `${getPercentage(count, reviewCount)}%` }} /></div>
            <strong>{formatNumber(count)}</strong>
          </div>;
        })}
      </div>
    </div>
  );
}

export default ShopRatingSummary;
