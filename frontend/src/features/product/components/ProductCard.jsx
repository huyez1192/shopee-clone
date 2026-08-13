import { Link } from "react-router-dom";

import styles from "./ProductCard.module.css";


import fallbackProductImage from "../../../assets/images/product-placeholder.png";

function formatPrice(price) {
  if (price == null) {
    return "";
  }

  return new Intl.NumberFormat("vi-VN").format(price);
}

function ProductCard({ product }) {
  const hasPriceRange =
    Number(product.minPrice) !== Number(product.maxPrice);

  const isOutOfStock = Number(product.totalStock) <= 0;

  return (
    <Link
      to={`/products/${product.id}`}
      className={styles.card}
    >
      <div className={styles.imageWrapper}>
{/*         <img */}
{/*           src={product.imageUrl} */}
{/*           alt={product.name} */}
{/*           className={styles.image} */}
{/*           loading="lazy" */}
{/*         /> */}
        <img           /* dùng tạm cái này vì img trong db đang là seed data: "imageUrl": "https://example.com/iphone15.jpg" */
          src={product.imageUrl || fallbackProductImage}
          alt={product.name}
          className={styles.image}
          loading="lazy"
          onError={(event) => {
            event.currentTarget.src = fallbackProductImage;
          }}
        />



        {isOutOfStock && (
          <div className={styles.outOfStock}>
            Hết hàng
          </div>
        )}
      </div>

      <div className={styles.content}>
        <h3 className={styles.name}>
          {product.name}
        </h3>

        <div className={styles.price}>
          <span className={styles.currency}>
            ₫
          </span>

          {hasPriceRange ? (
            <>
              <span>
                {formatPrice(product.minPrice)}
              </span>

              <span className={styles.priceSeparator}>
                -
              </span>

              <span>
                {formatPrice(product.maxPrice)}
              </span>
            </>
          ) : (
            <span>
              {formatPrice(product.minPrice)}
            </span>
          )}
        </div>

        <div className={styles.bottom}>
          <span className={styles.shopName}>
            {product.shopName}
          </span>
        </div>
      </div>
    </Link>
  );
}

export default ProductCard;