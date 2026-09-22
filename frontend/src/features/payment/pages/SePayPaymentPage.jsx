import { useEffect, useMemo, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useLocation, useNavigate } from "react-router-dom";

import { queryKeys } from "../../../query/queryKeys";
import { getPaymentStatus } from "../api/paymentApi";

import styles from "./SePayPaymentPage.module.css";

function formatPrice(price) {
  return new Intl.NumberFormat("vi-VN").format(Number(price) || 0);
}

function SePayPaymentPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const payment = useMemo(() => {
    if (location.state?.payment) {
      return location.state.payment;
    }

    const storedPayment = sessionStorage.getItem("sepayPayment");

    if (!storedPayment) {
      return null;
    }

    try {
      return JSON.parse(storedPayment);
    } catch {
      return null;
    }
  }, [location.state]);

  const [remainingSeconds, setRemainingSeconds] = useState(0);

  const {
    data: paymentStatusData,
    error: paymentStatusError,
  } = useQuery({
    queryKey: queryKeys.payment.detail(payment?.paymentCode),
    queryFn: () => getPaymentStatus(payment.paymentCode),
    enabled: Boolean(payment?.paymentCode) && payment?.status !== "PAID",
    refetchInterval: (query) => query.state.data?.status === "PAID" ? false : 3000,
    refetchIntervalInBackground: true,
  });

  const paymentStatus = paymentStatusData?.status || payment?.status || "PENDING";

  useEffect(() => {
    if (!paymentStatusError) {
      return;
    }

    console.error(
      "Không thể kiểm tra trạng thái thanh toán:",
      paymentStatusError,
    );
  }, [paymentStatusError]);

  useEffect(() => {
    if (paymentStatusData?.status !== "PAID") {
      return;
    }

    sessionStorage.removeItem("sepayPayment");

    queryClient.invalidateQueries({
      queryKey: queryKeys.orders.all,
    });

    navigate("/payment/success", {
      replace: true,
      state: {
        payment: paymentStatusData,
      },
    });
  }, [paymentStatusData, navigate, queryClient]);

  useEffect(() => {
    if (!payment?.expiresAt) {
      return;
    }

    function updateRemainingTime() {
      const expiresAt = new Date(payment.expiresAt).getTime();
      const now = Date.now();

      const difference = Math.max(
        0,
        Math.floor((expiresAt - now) / 1000),
      );

      setRemainingSeconds(difference);
    }

    updateRemainingTime();

    const intervalId = setInterval(updateRemainingTime, 1000);

    return () => clearInterval(intervalId);
  }, [payment]);

  if (!payment) {
    return (
      <main className={styles.page}>
        <div className={styles.card}>
          <h1>
            Không tìm thấy thông tin thanh toán
          </h1>

          <p>
            Phiên thanh toán không tồn tại hoặc
            đã bị mất.
          </p>

          <Link to="/orders">
            Xem đơn hàng
          </Link>
        </div>
      </main>
    );
  }

  const minutes = Math.floor(remainingSeconds / 60);

  const seconds = remainingSeconds % 60;

  const isExpired = remainingSeconds <= 0;

  return (
    <main className={styles.page}>
      <div className={styles.card}>
        <div className={styles.header}>
          <h1>
            Thanh toán qua SePay
          </h1>

          <p>
            Quét mã QR bằng ứng dụng ngân hàng
            để hoàn tất thanh toán.
          </p>
        </div>

        <div className={styles.content}>
          <div className={styles.qrSection}>
            <img
              src={payment.qrUrl}
              alt="QR thanh toán SePay"
              className={styles.qrImage}
            />

            {!isExpired ? (
              <div className={styles.waiting}>
                Đang chờ thanh toán...
              </div>
            ) : (
              <div className={styles.expired}>
                Phiên thanh toán đã hết hạn.
              </div>
            )}
          </div>

          <div className={styles.paymentInfo}>
            <div className={styles.infoRow}>
              <span>Ngân hàng</span>

              <strong>
                {payment.bankCode}
              </strong>
            </div>

            <div className={styles.infoRow}>
              <span>Chủ tài khoản</span>

              <strong>
                {payment.accountName}
              </strong>
            </div>

            <div className={styles.infoRow}>
              <span>Số tài khoản</span>

              <strong>
                {payment.accountNumber}
              </strong>
            </div>

            <div className={styles.infoRow}>
              <span>Số tiền</span>

              <strong
                className={styles.amount}
              >
                ₫{formatPrice(payment.amount)}
              </strong>
            </div>

            <div className={styles.infoRow}>
              <span>Nội dung chuyển khoản</span>

              <strong
                className={styles.paymentCode}
              >
                {payment.paymentCode}
              </strong>
            </div>

            <div className={styles.infoRow}>
              <span>Trạng thái</span>

              <strong className={styles.pending}>
                {paymentStatus}
              </strong>
            </div>

            <div className={styles.infoRow}>
              <span>Thời gian còn lại</span>

              <strong
                className={
                  isExpired
                    ? styles.expiredText
                    : styles.countdown
                }
              >
                {isExpired
                  ? "Đã hết hạn"
                  : `${String(
                      minutes,
                    ).padStart(
                      2,
                      "0",
                    )}:${String(
                      seconds,
                    ).padStart(
                      2,
                      "0",
                    )}`}
              </strong>
            </div>
          </div>
        </div>

        <div className={styles.notice}>
          <strong>Lưu ý:</strong>

          <span>
            Vui lòng chuyển đúng số tiền và giữ
            nguyên nội dung chuyển khoản để hệ
            thống có thể xác định giao dịch.
          </span>
        </div>

        <div className={styles.actions}>
          <Link
            to="/orders"
            className={styles.orderLink}
          >
            Xem đơn hàng
          </Link>
        </div>
      </div>
    </main>
  );
}

export default SePayPaymentPage;