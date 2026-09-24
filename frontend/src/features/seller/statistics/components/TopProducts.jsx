import fallbackProductImage from "../../../../assets/images/product-placeholder.png";
import { formatCurrency, formatNumber } from "../utils/statisticsUtils";
import styles from "../pages/SellerStatisticsPage.module.css";

function TopProducts({ products }) {
  return <div className={styles.productList}>{products.map((product, index) => (
    <article key={product.productId} className={styles.productRow}>
      <span className={styles.rank}>{index + 1}</span>
      <img
        src={product.imageUrl || fallbackProductImage}
        alt={product.productName}
        onError={(event) => { event.currentTarget.onerror = null; event.currentTarget.src = fallbackProductImage; }}
      />
      <div className={styles.productInfo}>
        <strong>{product.productName}</strong>
        <span>Đã bán {formatNumber(product.unitsSold)} sản phẩm</span>
      </div>
      <div className={styles.productRevenue}><span>Doanh thu</span><strong>{formatCurrency(product.merchandiseValue)}</strong></div>
    </article>
  ))}</div>;
}

export default TopProducts;
