import { Link, useLocation } from "react-router-dom";

import styles from "./PaymentSuccessPage.module.css";

function formatPrice(price) {
  return new Intl.NumberFormat("vi-VN").format(Number(price) || 0);
}

function PaymentSuccessPage() {
  const location = useLocation();

  const payment = location.state?.payment;

  return (
    <main className={styles.page}>
      <div className={styles.card}>
        <div className={styles.icon}>
          ✓
        </div>

        <h1>Thanh toán thành công</h1>

        <p>
          Shopee đã xác nhận giao dịch của bạn.
        </p>

        {payment && (
          <div className={styles.info}>
            <div>
              <span>Mã thanh toán</span>

              <strong>
                {payment.paymentCode}
              </strong>
            </div>

            <div>
              <span>Số tiền</span>

              <strong>
                ₫{formatPrice(payment.amount)}
              </strong>
            </div>

            <div>
              <span>Trạng thái</span>

              <strong>
                {payment.status}
              </strong>
            </div>
          </div>
        )}

        <div className={styles.actions}>
          <Link
            to="/orders"
            className={styles.orderButton}
          >
            Xem đơn hàng
          </Link>

          <Link
            to="/"
            className={styles.homeButton}
          >
            Tiếp tục mua sắm
          </Link>
        </div>
      </div>
    </main>
  );
}

export default PaymentSuccessPage;