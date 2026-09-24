import { useQuery } from "@tanstack/react-query";
import { Link, Navigate, useParams } from "react-router-dom";

import ErrorState from "../../../components/common/ErrorState";
import LoadingState from "../../../components/common/LoadingState";
import { queryKeys } from "../../../query/queryKeys";
import { getApiErrorMessage } from "../../../utils/getApiErrorMessage";
import { getPayment } from "../api/paymentApi";
import styles from "./PaymentSuccessPage.module.css";

function formatPrice(price) {
  return new Intl.NumberFormat("vi-VN").format(Number(price) || 0);
}

function formatPaidAt(value) {
  if (!value) return "";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "" : date.toLocaleString("vi-VN");
}

function PaymentSuccessPage() {
  const { paymentCode } = useParams();
  const paymentQuery = useQuery({
    queryKey: queryKeys.payment.detail(paymentCode),
    queryFn: () => getPayment(paymentCode),
    enabled: Boolean(paymentCode),
  });

  if (paymentQuery.isLoading) {
    return <LoadingState message="Đang xác nhận thanh toán..." />;
  }

  if (paymentQuery.error || !paymentQuery.data) {
    return (
      <ErrorState
        message={getApiErrorMessage(paymentQuery.error, "Không thể xác minh thanh toán.")}
        onRetry={paymentQuery.refetch}
      />
    );
  }

  const payment = paymentQuery.data;
  if (payment.status !== "PAID") {
    return <Navigate to={`/payment/sepay/${encodeURIComponent(paymentCode)}`} replace />;
  }

  return (
    <main className={styles.page}>
      <div className={styles.card}>
        <div className={styles.icon}>✓</div>
        <h1>Thanh toán thành công</h1>
        <p>Shopee đã xác nhận giao dịch của bạn.</p>

        <div className={styles.info}>
          <div><span>Mã thanh toán</span><strong>{payment.paymentCode}</strong></div>
          <div><span>Số tiền</span><strong>₫{formatPrice(payment.amount)}</strong></div>
          <div><span>Trạng thái</span><strong>{payment.status}</strong></div>
          {payment.paidAt && (
            <div><span>Thanh toán lúc</span><strong>{formatPaidAt(payment.paidAt)}</strong></div>
          )}
          {payment.orders?.map((order) => (
            <div key={order.id}>
              <span>Đơn hàng</span>
              <Link to={`/orders/${order.id}`}>{order.orderCode}</Link>
            </div>
          ))}
        </div>

        <div className={styles.actions}>
          <Link to="/orders" className={styles.orderButton}>Xem đơn hàng</Link>
          <Link to="/" className={styles.homeButton}>Tiếp tục mua sắm</Link>
        </div>
      </div>
    </main>
  );
}

export default PaymentSuccessPage;
