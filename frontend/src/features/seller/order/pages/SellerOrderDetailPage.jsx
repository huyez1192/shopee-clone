import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";

import fallbackProductImage from "../../../../assets/images/product-placeholder.png";
import { queryKeys } from "../../../../query/queryKeys";

import {
  completeSellerOrder,
  confirmSellerOrder,
  getSellerOrderById,
  shipSellerOrder,
} from "../api/sellerOrderApi";

import { getSellerOrderStatusLabel } from "../utils/sellerOrderUtils";

import styles from "./SellerOrderDetailPage.module.css";

function formatPrice(value) {
  return new Intl.NumberFormat("vi-VN").format(Number(value) || 0);
}

function formatDateTime(value) {
  if (!value) {
    return "-";
  }

  return new Intl.DateTimeFormat("vi-VN", {
    hour: "2-digit",
    minute: "2-digit",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(new Date(value));
}

function getTimelineSteps(order) {
  const steps = [
    {
      key: "created",
      title: "Đơn hàng mới",
      time: order.createdAt,
      active: true,
    },
    {
      key: "confirmed",
      title: "Đã xác nhận đơn hàng",
      time: order.confirmedAt,
      active: Boolean(order.confirmedAt),
    },
    {
      key: "shipping",
      title: "Đang giao hàng",
      time: order.shippedAt,
      active: Boolean(order.shippedAt),
    },
    {
      key: "completed",
      title: "Hoàn thành đơn hàng",
      time: order.completedAt,
      active: Boolean(order.completedAt),
    },
  ];

  if (order.status === "CANCELLED") {
    steps.push({
      key: "cancelled",
      title: "Đơn hàng đã hủy",
      time: order.cancelledAt,
      active: true,
    });
  }

  return steps;
}

function SellerOrderDetailPage() {
  const { orderId } = useParams();
  const queryClient = useQueryClient();

  const {
    data: order = null,
    isLoading,
    error,
    refetch,
  } = useQuery({
    queryKey: queryKeys.seller.orders.detail(orderId),
    queryFn: () => getSellerOrderById(orderId),
    enabled: Boolean(orderId),
  });

  const orderActionMutation = useMutation({
    mutationFn: async ({ orderId: targetOrderId, action }) => {
      if (action === "confirm") {
        return confirmSellerOrder(targetOrderId);
      }

      if (action === "ship") {
        return shipSellerOrder(targetOrderId);
      }

      if (action === "complete") {
        return completeSellerOrder(targetOrderId);
      }

      throw new Error("Hành động đơn hàng không hợp lệ.");
    },
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: queryKeys.seller.orders.all,
        }),
        queryClient.invalidateQueries({
          queryKey: queryKeys.orders.all,
        }),
      ]);
    },
  });

  const errorMessage = error
    ? error?.response?.data?.message || "Không thể tải chi tiết đơn hàng."
    : "";

  const isProcessing = orderActionMutation.isPending;

  function loadOrder() {
    refetch();
  }

  async function handleAction(action) {
    try {
      await orderActionMutation.mutateAsync({
        orderId: order.id,
        action,
      });
    } catch (error) {
      alert(
        error?.response?.data?.message ||
          error?.message ||
          "Không thể cập nhật đơn hàng.",
      );
    }
  }

  if (isLoading) {
    return (
      <div className={styles.stateBox}>
        Đang tải chi tiết đơn hàng...
      </div>
    );
  }

  if (errorMessage) {
    return (
      <div className={styles.stateBox}>
        <p>{errorMessage}</p>

        <button
          type="button"
          onClick={loadOrder}
          className={styles.retryButton}
        >
          Thử lại
        </button>
      </div>
    );
  }

  if (!order) {
    return (
      <div className={styles.stateBox}>
        Không tìm thấy đơn hàng.
      </div>
    );
  }

  const waitingForPayment =
    order.paymentMethod === "SEPAY" &&
    order.paymentStatus !== "PAID" &&
    order.status === "PENDING";

  const timelineSteps = getTimelineSteps(order);

  const currentStatusLabel = getSellerOrderStatusLabel(order);

  const trackingCode =
    order.trackingCode ||
    `SPXVN${String(order.id).padStart(10, "0")}`;

  const buyerName =
    order.buyerName || "Người mua";

  return (
    <section className={styles.page}>
      <div className={styles.breadcrumbs}>
        <Link to="/seller/orders">Đơn Hàng</Link>
        <span>›</span>
        <span>Chi tiết đơn hàng</span>
      </div>

      <div className={styles.grid}>
        <div className={styles.mainColumn}>
          <div className={styles.topRow}>
            <div className={styles.statusCard}>
              <div className={styles.cardIcon}>
                📋
              </div>

              <div>
                <h2>{currentStatusLabel}</h2>

                <p>
                  Đơn hiện đang ở trạng thái{" "}
                  <strong>
                    {currentStatusLabel}
                  </strong>
                  .
                </p>
              </div>
            </div>

            <div className={styles.noteCard}>
              <div className={styles.cardIcon}>
                📝
              </div>

              <div>
                <h3>Ghi chú</h3>

                <p>
                  {order.note || "Chưa có ghi chú cho đơn hàng này."}
                </p>
              </div>
            </div>
          </div>

          <div className={styles.infoCard}>
            <div className={styles.sectionHeader}>
              <h3>Lịch Sử Mua Hàng Của Người Mua</h3>
            </div>

            <div className={styles.infoRowBetween}>
              <div>
                <p className={styles.mutedText}>
                  Người mua:{" "}
                  <strong>{buyerName}</strong>
                </p>

                <p className={styles.mutedText}>
                  Đây là một đơn hàng của khách trong hệ thống shop.
                </p>
              </div>
            </div>
          </div>

          <div className={styles.infoCard}>
            <div className={styles.sectionBlock}>
              <h3>ID Đơn hàng</h3>

              <p className={styles.codeText}>
                {order.orderCode}
              </p>
            </div>

            <div className={styles.sectionBlock}>
              <h3>Địa chỉ nhận hàng</h3>

              <p className={styles.receiverName}>
                {order.address?.recipientName}
                {" - "}
                {order.address?.recipientPhone}
              </p>

              <p className={styles.addressText}>
                {[
                  order.address?.detailAddress,
                  order.address?.ward,
                  order.address?.province,
                ]
                  .filter(Boolean)
                  .join(", ")}
              </p>
            </div>

            <div className={styles.sectionBlock}>
              <h3>Thông tin vận chuyển</h3>

              <div className={styles.shippingRow}>
                <div className={styles.shippingMeta}>
                  <p>
                    <strong>Kiện hàng 1:</strong>{" "}
                    Nhanh
                  </p>

                  <p className={styles.mutedText}>
                    Vận chuyển nội bộ
                  </p>
                </div>

                <span className={styles.trackingBadge}>
                  #{trackingCode}
                </span>
              </div>
            </div>

            <div className={styles.sectionBlock}>
              <div className={styles.productSummary}>
                <span className={styles.productSummaryIcon}>
                  📦
                </span>

                <span>
                  Total{" "}
                  {order.items?.length || 0} products
                </span>
              </div>

              <div className={styles.timeline}>
                {timelineSteps.map((step) => (
                  <div
                    key={step.key}
                    className={styles.timelineItem}
                  >
                    <div
                      className={
                        step.active
                          ? styles.timelineDotActive
                          : styles.timelineDot
                      }
                    />

                    <div className={styles.timelineContent}>
                      <p
                        className={
                          step.active
                            ? styles.timelineTitleActive
                            : styles.timelineTitle
                        }
                      >
                        {step.title}
                      </p>

                      <p className={styles.timelineTime}>
                        {formatDateTime(step.time)}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className={styles.productsArea}>
              <h3>Sản phẩm trong đơn</h3>

              <div className={styles.productList}>
                {order.items?.map((item) => (
                  <div
                    key={item.id}
                    className={styles.productItem}
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

                    <div className={styles.productInfo}>
                      <strong>
                        {item.productName}
                      </strong>

                      <span>
                        Phân loại:{" "}
                        {[
                          item.variantValue1,
                          item.variantValue2,
                        ]
                          .filter(Boolean)
                          .join(", ") || "-"}
                      </span>

                      <span>
                        Số lượng: {item.quantity}
                      </span>
                    </div>

                    <div className={styles.productPrice}>
                      <span>
                        Đơn giá: ₫
                        {formatPrice(item.unitPrice)}
                      </span>

                      <strong>
                        Thành tiền: ₫
                        {formatPrice(item.lineTotal)}
                      </strong>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className={styles.summaryBox}>
              <div className={styles.summaryRow}>
                <span>Tổng tiền hàng</span>
                <span>
                  ₫
                  {formatPrice(
                    order.merchandiseSubtotal,
                  )}
                </span>
              </div>

              <div className={styles.summaryRow}>
                <span>Phí vận chuyển</span>
                <span>
                  ₫
                  {formatPrice(order.shippingFee)}
                </span>
              </div>

              <div className={styles.summaryRow}>
                <span>Phương thức thanh toán</span>
                <span>{order.paymentMethod}</span>
              </div>

              <div className={styles.summaryRow}>
                <span>Trạng thái thanh toán</span>
                <span>{order.paymentStatus}</span>
              </div>

              <div className={styles.summaryRowTotal}>
                <span>Tổng thanh toán</span>
                <strong>
                  ₫
                  {formatPrice(order.totalAmount)}
                </strong>
              </div>
            </div>

            <div className={styles.actionBar}>
              <Link
                to="/seller/orders"
                className={styles.backButton}
              >
                Quay lại
              </Link>

              {waitingForPayment && (
                <button
                  type="button"
                  disabled
                  className={styles.disabledButton}
                >
                  Chờ khách thanh toán
                </button>
              )}

              {!waitingForPayment &&
                order.status === "PENDING" && (
                  <button
                    type="button"
                    disabled={isProcessing}
                    className={styles.primaryButton}
                    onClick={() =>
                      handleAction("confirm")
                    }
                  >
                    {isProcessing
                      ? "Đang xử lý..."
                      : "Xác nhận đơn"}
                  </button>
                )}

              {order.status === "CONFIRMED" && (
                <button
                  type="button"
                  disabled={isProcessing}
                  className={styles.primaryButton}
                  onClick={() =>
                    handleAction("ship")
                  }
                >
                  {isProcessing
                    ? "Đang xử lý..."
                    : "Giao hàng"}
                </button>
              )}

              {order.status === "SHIPPING" && (
                <button
                  type="button"
                  disabled={isProcessing}
                  className={styles.primaryButton}
                  onClick={() =>
                    handleAction("complete")
                  }
                >
                  {isProcessing
                    ? "Đang xử lý..."
                    : "Hoàn thành"}
                </button>
              )}
            </div>
          </div>
        </div>

        <aside className={styles.sideColumn}>
          <div className={styles.sideCard}>
            <h4>Lịch sử đơn hàng</h4>

            <div className={styles.sideTimeline}>
              <div className={styles.sideTimelineItem}>
                <div className={styles.sideTimelineDotActive} />
                <div>
                  <p className={styles.sideTimelineTitle}>
                    Đơn hàng mới
                  </p>
                  <p className={styles.sideTimelineTime}>
                    {formatDateTime(order.createdAt)}
                  </p>
                </div>
              </div>

              {order.confirmedAt && (
                <div className={styles.sideTimelineItem}>
                  <div className={styles.sideTimelineDotActive} />
                  <div>
                    <p className={styles.sideTimelineTitle}>
                      Đã xác nhận
                    </p>
                    <p className={styles.sideTimelineTime}>
                      {formatDateTime(order.confirmedAt)}
                    </p>
                  </div>
                </div>
              )}

              {order.shippedAt && (
                <div className={styles.sideTimelineItem}>
                  <div className={styles.sideTimelineDotActive} />
                  <div>
                    <p className={styles.sideTimelineTitle}>
                      Đã gửi hàng
                    </p>
                    <p className={styles.sideTimelineTime}>
                      {formatDateTime(order.shippedAt)}
                    </p>
                  </div>
                </div>
              )}

              {order.completedAt && (
                <div className={styles.sideTimelineItem}>
                  <div className={styles.sideTimelineDotActive} />
                  <div>
                    <p className={styles.sideTimelineTitle}>
                      Hoàn thành
                    </p>
                    <p className={styles.sideTimelineTime}>
                      {formatDateTime(order.completedAt)}
                    </p>
                  </div>
                </div>
              )}

              {order.cancelledAt && (
                <div className={styles.sideTimelineItem}>
                  <div className={styles.sideTimelineDotCancel} />
                  <div>
                    <p className={styles.sideTimelineTitleCancel}>
                      Đã hủy đơn hàng
                    </p>
                    <p className={styles.sideTimelineTime}>
                      {formatDateTime(order.cancelledAt)}
                    </p>
                  </div>
                </div>
              )}
            </div>
          </div>
        </aside>
      </div>
    </section>
  );
}

export default SellerOrderDetailPage;