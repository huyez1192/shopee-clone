import { useQuery } from "@tanstack/react-query";
import { Link, useNavigate, useParams } from "react-router-dom";

import fallbackProductImage from "../../../assets/images/product-placeholder.png";

import LoadingState from "../../../components/common/LoadingState";
import ErrorState from "../../../components/common/ErrorState";

import { queryKeys } from "../../../query/queryKeys";
import { getOrderById } from "../api/orderApi";

import styles from "./OrderDetailPage.module.css";

function formatPrice(value) {
  return new Intl.NumberFormat("vi-VN").format(Number(value) || 0);
}

function formatDateTime(value) {
  if (!value) {
    return "";
  }

  return new Intl.DateTimeFormat("vi-VN", {
    hour: "2-digit",
    minute: "2-digit",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(new Date(value));
}

function getOrderStatusLabel(order) {
  if (order.status === "CANCELLED") {
    return "ĐƠN HÀNG ĐÃ HỦY";
  }

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
      return "NGƯỜI BÁN ĐÃ XÁC NHẬN ĐƠN HÀNG";

    case "SHIPPING":
      return "ĐƠN HÀNG ĐANG ĐƯỢC GIAO";

    case "COMPLETED":
      return "ĐƠN HÀNG ĐÃ HOÀN THÀNH";

    default:
      return order.status;
  }
}

function getCurrentStep(order) {
  switch (order.status) {
    case "PENDING":
      return 1;

    case "CONFIRMED":
      return 2;

    case "SHIPPING":
      return 3;

    case "COMPLETED":
      return 5;

    default:
      return 1;
  }
}

function OrderDetailPage() {
  const { orderId } = useParams();
  const navigate = useNavigate();

  const {
    data: order = null,
    isLoading,
    error,
    refetch,
  } = useQuery({
    queryKey: queryKeys.orders.detail(orderId),
    queryFn: () => getOrderById(orderId),
    enabled: Boolean(orderId),
  });

  const errorMessage = error
    ? error?.response?.data?.message || "Không thể tải chi tiết đơn hàng."
    : "";

  function loadOrder() {
    refetch();
  }

  if (isLoading) {
    return (
      <LoadingState
        message="Đang tải đơn hàng..."
      />
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
    return null;
  }

  const cancelled = order.status === "CANCELLED";
  const currentStep = getCurrentStep(order);

  const steps = [
    {
      number: 1,
      icon: "▤",
      label: "Đơn Hàng Đã Đặt",
      time: order.createdAt,
    },
    {
      number: 2,
      icon: "✓",
      label: "Đã Xác Nhận",
      time: order.confirmedAt,
    },
    {
      number: 3,
      icon: "▣",
      label: "Vận Chuyển",
      time: order.shippedAt,
    },
    {
      number: 4,
      icon: "⌂",
      label: "Đã Nhận Được Hàng",
      time: order.completedAt,
    },
    {
      number: 5,
      icon: "★",
      label: "Hoàn Thành",
      time: order.completedAt,
    },
  ];

  return (
    <main className={styles.page}>
      <div className={styles.container}>
        {/* ================= SIDEBAR ================= */}

        <aside className={styles.sidebar}>
          <div className={styles.user}>
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

          <nav className={styles.menu}>
            <Link to="/profile">
              👤 Tài Khoản Của Tôi
            </Link>

            <Link to="/addresses">
              📍 Địa Chỉ
            </Link>

            <Link
              to="/orders"
              className={styles.menuActive}
            >
              📋 Đơn Mua
            </Link>
          </nav>
        </aside>

        {/* ================= CONTENT ================= */}

        <section className={styles.content}>
          <div className={styles.topBar}>
            <button
              type="button"
              onClick={() => navigate(-1)}
            >
              ‹ TRỞ LẠI
            </button>

            <div>
              <span>
                MÃ ĐƠN HÀNG.{" "}
                {order.orderCode}
              </span>

              <i />

              <strong>
                {getOrderStatusLabel(order)}
              </strong>
            </div>
          </div>

          {/* ================= CANCELLED ================= */}

          {cancelled ? (
            <>
              <section
                className={styles.cancelBanner}
              >
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
              </section>

              <OrderProducts order={order} />

              <section
                className={styles.cancelDetails}
              >
                <div>
                  <span>Yêu cầu bởi</span>

                  <strong>
                    {order.cancelledBy ===
                    "BUYER"
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
                    {order.paymentMethod ===
                    "SEPAY"
                      ? "Chuyển khoản SePay"
                      : "Thanh toán khi nhận hàng"}
                  </strong>
                </div>

                <div>
                  <span>
                    Trạng thái thanh toán
                  </span>

                  <strong>
                    {order.paymentStatus}
                  </strong>
                </div>

                <div>
                  <span>Mã đơn hàng</span>

                  <strong
                    className={
                      styles.orderCodeValue
                    }
                  >
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
            </>
          ) : (
            <>
              {/* ================= PROGRESS ================= */}

              <section
                className={styles.progress}
              >
                <div
                  className={
                    styles.progressBackground
                  }
                />

                <div
                  className={
                    styles.progressActive
                  }
                  style={{
                    width:
                      currentStep === 1
                        ? "0%"
                        : `${((currentStep - 1) / 4) * 80}%`,
                  }}
                />

                {steps.map((step) => {
                  const active =
                    currentStep >=
                    step.number;

                  return (
                    <div
                      key={step.number}
                      className={styles.step}
                    >
                      <div
                        className={
                          active
                            ? styles.stepIconActive
                            : styles.stepIcon
                        }
                      >
                        {step.icon}
                      </div>

                      <strong>
                        {step.label}
                      </strong>

                      <span>
                        {active &&
                        step.time
                          ? formatDateTime(
                              step.time,
                            )
                          : ""}
                      </span>
                    </div>
                  );
                })}
              </section>

              {/* ================= ACTION BAR ================= */}

              <section
                className={styles.actionBar}
              >
                <div>
                  {order.status ===
                    "SHIPPING" && (
                    <p>
                      Đơn hàng đang trên đường
                      giao đến bạn.
                    </p>
                  )}

                  {order.status ===
                    "COMPLETED" && (
                    <p>
                      Đơn hàng đã được giao
                      thành công.
                    </p>
                  )}

                  {order.status ===
                    "PENDING" && (
                    <p>
                      Đơn hàng đang chờ người
                      bán xác nhận.
                    </p>
                  )}
                </div>

                <div
                  className={
                    styles.actionButtons
                  }
                >
                  {order.status ===
                    "COMPLETED" && (
                    <Link
                      to="/products"
                      className={
                        styles.primaryButton
                      }
                    >
                      Mua Lại
                    </Link>
                  )}

                  <Link
                    to={`/shops/${order.shopId}`}
                    className={
                      styles.secondaryButton
                    }
                  >
                    Xem Shop
                  </Link>
                </div>
              </section>

              {/* ================= ADDRESS ================= */}

              <section
                className={styles.delivery}
              >
                <div
                  className={
                    styles.deliveryStripe
                  }
                />

                <div
                  className={
                    styles.addressColumn
                  }
                >
                  <h2>
                    Địa Chỉ Nhận Hàng
                  </h2>

                  <strong>
                    {order.recipientName}
                  </strong>

                  <span>
                    {order.recipientPhone}
                  </span>

                  <p>
                    {[
                      order.detailAddress,
                      order.ward,
                      order.province,
                    ]
                      .filter(Boolean)
                      .join(", ")}
                  </p>
                </div>

                <div
                  className={
                    styles.deliveryTimeline
                  }
                >
                  {order.completedAt && (
                    <TimelineItem
                      time={
                        order.completedAt
                      }
                      title="Đã giao"
                      description="Giao hàng thành công"
                      active
                    />
                  )}

                  {order.shippedAt && (
                    <TimelineItem
                      time={
                        order.shippedAt
                      }
                      title="Đang vận chuyển"
                      description="Đơn hàng đang được giao"
                    />
                  )}

                  {order.confirmedAt && (
                    <TimelineItem
                      time={
                        order.confirmedAt
                      }
                      title="Đã xác nhận"
                      description="Người bán đã chuẩn bị đơn hàng"
                    />
                  )}

                  <TimelineItem
                    time={order.createdAt}
                    title="Đơn hàng đã được đặt"
                  />
                </div>
              </section>

              <OrderProducts order={order} />

              {/* ================= SUMMARY ================= */}

              <section
                className={styles.summary}
              >
                <SummaryRow
                  label="Tổng tiền hàng"
                  value={`₫${formatPrice(
                    order.merchandiseSubtotal,
                  )}`}
                />

                <SummaryRow
                  label="Phí vận chuyển"
                  value={`₫${formatPrice(
                    order.shippingFee,
                  )}`}
                />

                <SummaryRow
                  label="Thành tiền"
                  value={`₫${formatPrice(
                    order.totalAmount,
                  )}`}
                  total
                />

                <SummaryRow
                  label="Phương thức Thanh toán"
                  value={
                    order.paymentMethod ===
                    "SEPAY"
                      ? "Chuyển khoản QR - SePay"
                      : "Thanh toán khi nhận hàng"
                  }
                />

                <SummaryRow
                  label="Trạng thái thanh toán"
                  value={
                    order.paymentStatus ===
                    "PAID"
                      ? "Đã thanh toán"
                      : "Chưa thanh toán"
                  }
                />
              </section>
            </>
          )}
        </section>
      </div>
    </main>
  );
}

function TimelineItem({
  time,
  title,
  description,
  active = false,
}) {
  return (
    <div
      className={
        active
          ? styles.timelineItemActive
          : styles.timelineItem
      }
    >
      <div className={styles.timelineDot} />

      <span>
        {formatDateTime(time)}
      </span>

      <div>
        <strong>{title}</strong>

        {description && (
          <p>{description}</p>
        )}
      </div>
    </div>
  );
}

function OrderProducts({ order }) {
  return (
    <section className={styles.products}>
      <div className={styles.shopHeader}>
        <div>
          <span>
            Yêu thích
          </span>

          <strong>
            {order.shopName}
          </strong>
        </div>

        <Link
          to={`/shops/${order.shopId}`}
        >
          Xem Shop
        </Link>
      </div>

      {order.items?.map((item) => {
        const variant = [
          item.variantValue1,
          item.variantValue2,
        ]
          .filter(Boolean)
          .join(", ");

        return (
          <Link
            key={item.id}
            to={`/products/${item.productId}`}
            className={styles.product}
          >
            <img
              src={
                item.productImageUrl ||
                fallbackProductImage
              }
              alt={item.productName}
              onError={(event) => {
                event.currentTarget.src =
                  fallbackProductImage;
              }}
            />

            <div
              className={
                styles.productInfo
              }
            >
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

            <div
              className={
                styles.productPrice
              }
            >
              ₫
              {formatPrice(
                item.unitPrice,
              )}
            </div>
          </Link>
        );
      })}
    </section>
  );
}

function SummaryRow({
  label,
  value,
  total = false,
}) {
  return (
    <div
      className={
        total
          ? styles.summaryTotal
          : styles.summaryRow
      }
    >
      <span>{label}</span>

      <strong>{value}</strong>
    </div>
  );
}

export default OrderDetailPage;