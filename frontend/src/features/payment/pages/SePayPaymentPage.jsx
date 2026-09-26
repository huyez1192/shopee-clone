import { useEffect, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useNavigate, useParams } from "react-router-dom";

import ErrorState from "../../../components/common/ErrorState";
import LoadingState from "../../../components/common/LoadingState";
import { queryKeys } from "../../../query/queryKeys";
import { getApiErrorMessage } from "../../../utils/getApiErrorMessage";
import { getPayment, getPaymentStatus } from "../api/paymentApi";
import styles from "./SePayPaymentPage.module.css";

const TERMINAL_STATUSES = new Set(["PAID", "EXPIRED", "FAILED"]);

function formatPrice(price) {
  return new Intl.NumberFormat("vi-VN").format(Number(price) || 0);
}

function SePayPaymentPage() {
  const { paymentCode } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [remainingSeconds, setRemainingSeconds] = useState(0);
  const [copied, setCopied] = useState(false);

  const paymentQuery = useQuery({
    queryKey: queryKeys.payment.detail(paymentCode),
    queryFn: () => getPayment(paymentCode),
    enabled: Boolean(paymentCode),
  });

  const detailStatus = paymentQuery.data?.status;
  const statusQuery = useQuery({
    queryKey: queryKeys.payment.status(paymentCode),
    queryFn: () => getPaymentStatus(paymentCode),
    enabled: Boolean(paymentCode) && detailStatus === "PENDING",
    refetchInterval: (query) =>
      query.state.data?.status && query.state.data.status !== "PENDING"
        ? false
        : 3000,
    refetchIntervalInBackground: true,
    retry: 1,
  });

  const payment = paymentQuery.data;
  const latestStatus = statusQuery.data?.status || detailStatus;
  const expiresAt = statusQuery.data?.expiresAt || payment?.expiresAt;

  useEffect(() => {
    if (!expiresAt) {
      return undefined;
    }

    function updateRemainingTime() {
      const difference = Math.max(
        0,
        Math.floor((new Date(expiresAt).getTime() - Date.now()) / 1000),
      );
      setRemainingSeconds(difference);
    }

    updateRemainingTime();
    const intervalId = window.setInterval(updateRemainingTime, 1000);
    return () => window.clearInterval(intervalId);
  }, [expiresAt]);

  useEffect(() => {
    if (latestStatus !== "PAID") {
      return;
    }

    queryClient.setQueryData(
      queryKeys.payment.detail(paymentCode),
      (currentPayment) => ({ ...currentPayment, ...statusQuery.data }),
    );
    queryClient.invalidateQueries({ queryKey: queryKeys.orders.all });
    queryClient.invalidateQueries({
      queryKey: queryKeys.payment.detail(paymentCode),
      refetchType: "none",
    });
    navigate(`/payment/success/${encodeURIComponent(paymentCode)}`, { replace: true });
  }, [latestStatus, navigate, paymentCode, queryClient, statusQuery.data]);

  if (paymentQuery.isLoading) {
    return <LoadingState message="Đang tải thông tin thanh toán..." />;
  }

  if (paymentQuery.error || !payment) {
    return (
      <ErrorState
        message={getApiErrorMessage(paymentQuery.error, "Không thể tải thông tin thanh toán.")}
        onRetry={paymentQuery.refetch}
      />
    );
  }

  const minutes = Math.floor(remainingSeconds / 60);
  const seconds = remainingSeconds % 60;
  const awaitingBackendExpiration = latestStatus === "PENDING" && remainingSeconds === 0;
  const isTerminal = TERMINAL_STATUSES.has(latestStatus);

  async function copyPaymentCode() {
    try {
      await navigator.clipboard.writeText(payment.paymentCode);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1800);
    } catch {
      setCopied(false);
    }
  }

  return (
    <main className={styles.page}>
      <div className={styles.card}>
        <div className={styles.header}>
          <h1>Thanh toán qua SePay</h1>
          <p>Quét mã QR bằng ứng dụng ngân hàng để hoàn tất thanh toán.</p>
        </div>

        {latestStatus === "EXPIRED" || latestStatus === "FAILED" ? (
          <div className={styles.notice} role="status">
            <strong>
              {latestStatus === "EXPIRED"
                ? "Phiên thanh toán đã hết hạn."
                : "Thanh toán không thành công."}
            </strong>
            <span>
              {latestStatus === "EXPIRED"
                ? "Đơn hàng chưa thanh toán đã được hủy."
                : "Bạn có thể xem lại trạng thái đơn hàng hoặc quay lại mua hàng."}
            </span>
          </div>
        ) : (
          <div className={styles.content}>
            <div className={styles.qrSection}>
              <img src={payment.qrUrl} alt="QR thanh toán SePay" className={styles.qrImage} />
              <div className={styles.waiting}>
                {awaitingBackendExpiration
                  ? "Đang xác nhận trạng thái thanh toán..."
                  : "Đang chờ thanh toán..."}
              </div>
            </div>

            <div className={styles.paymentInfo}>
              <InfoRow label="Ngân hàng" value={payment.bankCode} />
              <InfoRow label="Chủ tài khoản" value={payment.accountName} />
              <InfoRow label="Số tài khoản" value={payment.accountNumber} />
              <InfoRow label="Số tiền" value={`₫${formatPrice(payment.amount)}`} valueClass={styles.amount} />
              <div className={styles.infoRow}>
                <span>Nội dung chuyển khoản</span>
                <span className={styles.codeWithAction}>
                  <strong className={styles.paymentCode}>{payment.paymentCode}</strong>
                  <button type="button" onClick={copyPaymentCode} aria-label="Sao chép nội dung chuyển khoản">
                    {copied ? "Đã chép" : "Sao chép"}
                  </button>
                </span>
              </div>
              <InfoRow label="Trạng thái" value={latestStatus} valueClass={styles.pending} />
              <InfoRow
                label="Thời gian còn lại"
                value={awaitingBackendExpiration
                  ? "Đang xác nhận..."
                  : `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`}
                valueClass={awaitingBackendExpiration ? styles.expiredText : styles.countdown}
              />
            </div>
          </div>
        )}

        {statusQuery.error && latestStatus === "PENDING" && (
          <div className={styles.notice} role="alert">
            <span>Không thể cập nhật trạng thái thanh toán.</span>
            <button type="button" onClick={() => statusQuery.refetch()}>Thử lại</button>
          </div>
        )}

        {!isTerminal && (
          <div className={styles.notice}>
            <strong>Lưu ý:</strong>
            <span>Vui lòng chuyển đúng số tiền và giữ nguyên nội dung chuyển khoản.</span>
          </div>
        )}

        {Array.isArray(payment.orders) && payment.orders.length > 0 && (
          <div className={styles.notice}>
            <strong>Đơn hàng:</strong>
            {payment.orders.map((order) => (
              <Link key={order.id} to={`/orders/${order.id}`}>{order.orderCode}</Link>
            ))}
          </div>
        )}

        <div className={styles.actions}>
          <Link to="/orders" className={styles.orderLink}>Xem đơn hàng</Link>
          {isTerminal && <Link to="/">Quay lại mua hàng</Link>}
        </div>
      </div>
    </main>
  );
}

function InfoRow({ label, value, valueClass }) {
  return (
    <div className={styles.infoRow}>
      <span>{label}</span>
      <strong className={valueClass}>{value}</strong>
    </div>
  );
}

export default SePayPaymentPage;
