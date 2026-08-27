import {getCompletedStep,} from "../utils/orderStatusUtils";

import styles from "./OrderProgress.module.css";

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

function OrderProgress({ order }) {
  const currentStep =
    getCompletedStep(order);

  const isPaid =
    order.paymentStatus === "PAID";

  const steps = [
    {
      number: 1,
      icon: "▣",
      label: "Đơn Hàng Đã Đặt",
      time: order.createdAt,
    },
    {
      number: 2,
      icon: "₫",
      label:
        order.paymentMethod === "COD"
          ? "Thanh Toán COD"
          : "Đơn Hàng Đã Thanh Toán",
      time:
        isPaid &&
        order.paymentMethod === "COD"
          ? order.completedAt
          : null,
    },
    {
      number: 3,
      icon: "✓",
      label: "Người Bán Đã Xác Nhận",
      time: order.confirmedAt,
    },
    {
      number: 4,
      icon: "▣",
      label: "Đang Giao Hàng",
      time: order.shippedAt,
    },
    {
      number: 5,
      icon: "★",
      label: "Hoàn Thành",
      time: order.completedAt,
    },
  ];

  return (
    <div className={styles.progress}>
      <div className={styles.line} />

      <div
        className={styles.activeLine}
        style={{
          width:
            currentStep <= 1
              ? "0%"
              : `${
                  ((currentStep - 1) / 4) *
                  100
                }%`,
        }}
      />

      {steps.map((step) => {
        const active =
          currentStep >= step.number;

        return (
          <div
            key={step.number}
            className={styles.step}
          >
            <div
              className={
                active
                  ? styles.activeIcon
                  : styles.icon
              }
            >
              {step.icon}
            </div>

            <div className={styles.label}>
              {step.label}
            </div>

            <div className={styles.time}>
              {active && step.time
                ? formatDateTime(step.time)
                : ""}
            </div>
          </div>
        );
      })}
    </div>
  );
}

export default OrderProgress;