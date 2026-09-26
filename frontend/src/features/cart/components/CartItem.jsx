import { Link } from "react-router-dom";

import fallbackProductImage from "../../../assets/images/product-placeholder.png";

import styles from "./CartItem.module.css";

function formatPrice(price) {
  return new Intl.NumberFormat("vi-VN").format(
    Number(price) || 0,
  );
}

function CartItem({
  item,
  checked,
  isUpdating,
  isDeleting,
  onSelect,
  onIncrease,
  onDecrease,
  onDelete,
}) {
  const variantText = [
    item.variantValue1,
    item.variantValue2,
  ]
    .filter(Boolean)
    .join(", ");

  return (
    <div
      className={`${styles.item} ${
        !item.available ? styles.unavailable : ""
      }`}
    >
      <div className={styles.productColumn}>
        <input
          type="checkbox"
          checked={checked}
          disabled={!item.available}
          onChange={() => onSelect(item.id)}
          aria-label={`Chọn ${item.productName}`}
        />

        <Link
          to={`/products/${item.productId}`}
          className={styles.imageLink}
        >
          <img
            src={
              item.productImageUrl ||
              fallbackProductImage
            }
            alt={item.productName}
            className={styles.image}
            onError={(event) => {
              event.currentTarget.src =
                fallbackProductImage;
            }}
          />
        </Link>

        <div className={styles.productInfo}>
          <Link
            to={`/products/${item.productId}`}
            className={styles.productName}
          >
            {item.productName}
          </Link>

          {!item.available && (
            <span className={styles.unavailableMessage}>
              {item.unavailableReason ||
                "Sản phẩm hiện không khả dụng."}
            </span>
          )}
        </div>
      </div>

      <div className={styles.variantColumn} data-label="Phân loại">
        {variantText ? (
          <>
            <span className={styles.variantLabel}>
              Phân Loại Hàng:
            </span>

            <span>{variantText}</span>
          </>
        ) : (
          <span>—</span>
        )}
      </div>

      <div className={styles.priceColumn} data-label="Đơn giá">
        ₫{formatPrice(item.unitPrice)}
      </div>

      <div className={styles.quantityColumn} data-label="Số lượng">
        <button
          type="button"
          disabled={
            !item.available ||
            item.quantity <= 1 ||
            isUpdating
          }
          onClick={() => onDecrease(item)}
        >
          −
        </button>

        <span>{item.quantity}</span>

        <button
          type="button"
          disabled={
            !item.available ||
            item.quantity >= item.availableStock ||
            isUpdating
          }
          onClick={() => onIncrease(item)}
        >
          +
        </button>
      </div>

      <div className={styles.totalColumn} data-label="Thành tiền">
        ₫{formatPrice(item.lineTotal)}
      </div>

      <div className={styles.actionColumn}>
        <button
          type="button"
          disabled={isDeleting}
          onClick={() => onDelete(item)}
        >
          {isDeleting ? "Đang xóa..." : "Xóa"}
        </button>
      </div>
    </div>
  );
}

export default CartItem;
