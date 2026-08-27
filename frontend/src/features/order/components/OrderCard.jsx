import { Link } from "react-router-dom";

import fallbackProductImage from "../../../assets/images/product-placeholder.png";

import styles from "./OrderCard.module.css";

function formatPrice(price) {
  return new Intl.NumberFormat("vi-VN").format(
    Number(price) || 0,
  );
}

function getOrderStatusLabel(order) {
  if (
    order.paymentMethod === "SEPAY" &&
    order.paymentStatus === "UNPAID" &&
    order.status === "PENDING"
  ) {
    return "CHỜ THANH TOÁN";
  }

  switch (order.status) {
    case "PENDING":
      return "CHỜ XÁC NHẬN";

    case "CONFIRMED":
      return "ĐANG XỬ LÝ";

    case "SHIPPING":
      return "ĐANG GIAO";

    case "COMPLETED":
      return "HOÀN THÀNH";

    case "CANCELLED":
      return "ĐÃ HỦY";

    default:
      return order.status;
  }
}

function OrderCard({ order }) {
  return (
    <article className={styles.card}>
      <div className={styles.shopHeader}>
        <div className={styles.shopInfo}>
          <span className={styles.shopBadge}>
            Yêu thích
          </span>

          <strong>{order.shopName}</strong>

          <Link
            to={`/shops/${order.shopId}`}
            className={styles.shopButton}
          >
            Xem Shop
          </Link>
        </div>

        <div className={styles.statusArea}>
          {order.paymentMethod === "SEPAY" &&
            order.paymentStatus === "PAID" && (
              <span className={styles.paid}>
                ✓ Đã thanh toán
              </span>
            )}

          <span className={styles.status}>
            {getOrderStatusLabel(order)}
          </span>
        </div>
      </div>

      <Link
        to={`/orders/${order.id}`}
        className={styles.items}
      >
        {order.items.map((item) => {
          const variantText = [
            item.variantValue1,
            item.variantValue2,
          ]
            .filter(Boolean)
            .join(", ");

          return (
            <div
              key={item.id}
              className={styles.item}
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

              <div className={styles.itemInfo}>
                <div className={styles.productName}>
                  {item.productName}
                </div>

                {variantText && (
                  <div className={styles.variant}>
                    Phân loại hàng: {variantText}
                  </div>
                )}

                <div className={styles.quantity}>
                  x{item.quantity}
                </div>
              </div>

              <div className={styles.itemPrice}>
                ₫{formatPrice(item.unitPrice)}
              </div>
            </div>
          );
        })}
      </Link>

      <div className={styles.totalRow}>
        <span>Thành tiền:</span>

        <strong>
          ₫{formatPrice(order.totalAmount)}
        </strong>
      </div>

      <div className={styles.footer}>
        <div className={styles.orderMeta}>
          <span>
            Mã đơn: {order.orderCode}
          </span>

          <span>
            {order.paymentMethod === "SEPAY"
              ? "Thanh toán QR - SePay"
              : "Thanh toán khi nhận hàng"}
          </span>
        </div>

        <div className={styles.actions}>
          <Link
            to={`/orders/${order.id}`}
            className={styles.detailButton}
          >
            Xem chi tiết
          </Link>
        </div>
      </div>
    </article>
  );
}

export default OrderCard;