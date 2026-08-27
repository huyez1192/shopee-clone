import {useCallback, useEffect, useState,} from "react";

import {Link, useNavigate, useParams,} from "react-router-dom";

import EmptyState from "../../../components/common/EmptyState";
import ErrorState from "../../../components/common/ErrorState";
import LoadingState from "../../../components/common/LoadingState";

import fallbackProductImage from "../../../assets/images/product-placeholder.png";

import {getApiErrorMessage,} from "../../../utils/getApiErrorMessage";

import {getOrderById,} from "../api/orderApi";

import OrderProgress from "../components/OrderProgress";

import {getOrderStatusLabel,} from "../utils/orderStatusUtils";

import styles from "./OrderDetailPage.module.css";

function formatPrice(price) {
  return new Intl.NumberFormat("vi-VN").format(
    Number(price) || 0,
  );
}

function formatDateTime(value) {
  if (!value) {
    return "";
  }

  return new Intl.DateTimeFormat(
    "vi-VN",
    {
      hour: "2-digit",
      minute: "2-digit",
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    },
  ).format(new Date(value));
}

function OrderDetailPage() {
  const { orderId } = useParams();

  const navigate = useNavigate();

  const [order, setOrder] = useState(null);

  const [isLoading, setIsLoading] =
    useState(true);

  const [errorMessage, setErrorMessage] =
    useState("");

  const loadOrder = useCallback(
    async () => {
      try {
        setIsLoading(true);
        setErrorMessage("");

        const response =
          await getOrderById(orderId);

        setOrder(response);
      } catch (error) {
        setErrorMessage(
          getApiErrorMessage(
            error,
            "Không thể tải chi tiết đơn hàng.",
          ),
        );
      } finally {
        setIsLoading(false);
      }
    },
    [orderId],
  );

  useEffect(() => {
    loadOrder();
  }, [loadOrder]);

  if (isLoading) {
    return (
      <LoadingState message="Đang tải đơn hàng..." />
    );
  }

  if (errorMessage) {
    return (
      <ErrorState
        message={errorMessage}
        onRetry={loadOrder}
      />
    );
  }

  if (!order) {
    return (
      <EmptyState
        title="Không tìm thấy đơn hàng"
        description="Đơn hàng không tồn tại."
      />
    );
  }

  const cancelled =
    order.status === "CANCELLED";

  return (
    <main className={styles.page}>
      <div className={styles.container}>
        <aside className={styles.sidebar}>
          <div className={styles.profile}>
            <div className={styles.avatar}>
              👤
            </div>

            <div>
              <strong>
                Tài khoản của tôi
              </strong>

              <span>Sửa Hồ Sơ</span>
            </div>
          </div>

          <nav className={styles.sideNav}>
            <Link to="/profile">
              👤 Tài Khoản Của Tôi
            </Link>

            <Link to="/addresses">
              📍 Địa Chỉ
            </Link>

            <Link
              to="/orders"
              className={styles.sideActive}
            >
              📋 Đơn Mua
            </Link>
          </nav>
        </aside>

        <section className={styles.content}>
          <div className={styles.topBar}>
            <button
              type="button"
              onClick={() => navigate(-1)}
              className={styles.backButton}
            >
              ‹ TRỞ LẠI
            </button>

            <div className={styles.orderCode}>
              MÃ ĐƠN HÀNG. {order.orderCode}

              <span />

              <strong>
                {getOrderStatusLabel(order)}
              </strong>
            </div>
          </div>

          {cancelled ? (
            <div className={styles.cancelledBanner}>
              <strong>
                Đã hủy đơn hàng
              </strong>

              {order.cancelledAt && (
                <span>
                  vào{" "}
                  {formatDateTime(
                    order.cancelledAt,
                  )}
                </span>
              )}
            </div>
          ) : (
            <OrderProgress order={order} />
          )}

          {!cancelled && (
            <div className={styles.actionSection}>
              {order.status === "COMPLETED" && (
                <Link
                  to="/products"
                  className={styles.primaryButton}
                >
                  Mua Lại
                </Link>
              )}

              <Link
                to={`/shops/${order.shopId}`}
                className={styles.secondaryButton}
              >
                Liên Hệ / Xem Shop
              </Link>

              {/*
                Hủy đơn / refund chưa mở ở đây.
                Ta sẽ thêm sau khi hoàn thiện
                cancel + refund SePay.
              */}
            </div>
          )}

          {!cancelled && (
            <section className={styles.addressSection}>
              <div className={styles.addressStripe} />

              <div className={styles.addressInfo}>
                <h2>
                  Địa Chỉ Nhận Hàng
                </h2>

                <strong>
                  {order.address?.recipientName}
                </strong>

                <span>
                  {order.address?.recipientPhone}
                </span>

                <p>
                  {[
                    order.address?.detailAddress,
                    order.address?.ward,
                    order.address?.province,
                  ]
                    .filter(Boolean)
                    .join(", ")}
                </p>
              </div>

              <div className={styles.timeline}>
                {order.completedAt && (
                  <div className={styles.timelineActive}>
                    <span>
                      {formatDateTime(
                        order.completedAt,
                      )}
                    </span>

                    <strong>
                      Đơn hàng đã hoàn thành
                    </strong>
                  </div>
                )}

                {order.shippedAt && (
                  <div>
                    <span>
                      {formatDateTime(
                        order.shippedAt,
                      )}
                    </span>

                    <strong>
                      Đơn hàng đang được giao
                    </strong>
                  </div>
                )}

                {order.confirmedAt && (
                  <div>
                    <span>
                      {formatDateTime(
                        order.confirmedAt,
                      )}
                    </span>

                    <strong>
                      Người bán đã xác nhận đơn
                    </strong>
                  </div>
                )}

                <div>
                  <span>
                    {formatDateTime(
                      order.createdAt,
                    )}
                  </span>

                  <strong>
                    Đơn hàng đã được đặt
                  </strong>
                </div>
              </div>
            </section>
          )}

          <section className={styles.orderSection}>
            <div className={styles.shopHeader}>
              <div>
                <span className={styles.favorite}>
                  Yêu thích
                </span>

                <strong>
                  {order.shopName}
                </strong>
              </div>

              <Link
                to={`/shops/${order.shopId}`}
                className={styles.shopButton}
              >
                Xem Shop
              </Link>
            </div>

            <div className={styles.items}>
              {order.items.map((item) => {
                const variant =
                  [
                    item.variantValue1,
                    item.variantValue2,
                  ]
                    .filter(Boolean)
                    .join(", ");

                return (
                  <Link
                    key={item.id}
                    to={`/products/${item.productId}`}
                    className={styles.item}
                  >
                    <img
                      src={
                        item.productImageUrl ||
                        fallbackProductImage
                      }
                      alt={item.productName}
                      className={styles.productImage}
                      onError={(event) => {
                        event.currentTarget.src =
                          fallbackProductImage;
                      }}
                    />

                    <div className={styles.itemInfo}>
                      <strong>
                        {item.productName}
                      </strong>

                      {variant && (
                        <span>
                          Phân loại hàng:{" "}
                          {variant}
                        </span>
                      )}

                      <span>
                        x{item.quantity}
                      </span>
                    </div>

                    <div className={styles.itemPrice}>
                      ₫{formatPrice(
                        item.lineTotal,
                      )}
                    </div>
                  </Link>
                );
              })}
            </div>

            <div className={styles.summary}>
              <div>
                <span>Tổng tiền hàng</span>

                <strong>
                  ₫{formatPrice(
                    order.merchandiseSubtotal,
                  )}
                </strong>
              </div>

              <div>
                <span>Phí vận chuyển</span>

                <strong>
                  ₫{formatPrice(
                    order.shippingFee,
                  )}
                </strong>
              </div>

              <div className={styles.grandTotal}>
                <span>Thành tiền</span>

                <strong>
                  ₫{formatPrice(
                    order.totalAmount,
                  )}
                </strong>
              </div>

              <div>
                <span>
                  Phương thức Thanh toán
                </span>

                <strong>
                  {order.paymentMethod === "SEPAY"
                    ? "Chuyển khoản QR - SePay"
                    : "Thanh toán khi nhận hàng"}
                </strong>
              </div>

              <div>
                <span>
                  Trạng thái thanh toán
                </span>

                <strong
                  className={
                    order.paymentStatus === "PAID"
                      ? styles.paid
                      : styles.unpaid
                  }
                >
                  {order.paymentStatus === "PAID"
                    ? "Đã thanh toán"
                    : "Chưa thanh toán"}
                </strong>
              </div>
            </div>
          </section>

          {cancelled && (
            <section className={styles.cancelInfo}>
              <div>
                <span>Yêu cầu bởi</span>

                <strong>
                  {order.cancelledBy === "BUYER"
                    ? "Người mua"
                    : order.cancelledBy ===
                        "SELLER"
                      ? "Người bán"
                      : "Hệ thống"}
                </strong>
              </div>

              <div>
                <span>
                  Phương thức thanh toán
                </span>

                <strong>
                  {order.paymentMethod === "SEPAY"
                    ? "SePay"
                    : "COD"}
                </strong>
              </div>

              <div>
                <span>Mã đơn hàng</span>

                <strong>
                  {order.orderCode}
                </strong>
              </div>

              {order.cancelReason && (
                <p>
                  <strong>Lý do:</strong>{" "}
                  {order.cancelReason}
                </p>
              )}
            </section>
          )}
        </section>
      </div>
    </main>
  );
}

export default OrderDetailPage;