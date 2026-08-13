import { Link, Outlet } from "react-router-dom";

import shopeeCheckoutLogo from "../assets/images/shopee-checkout-logo.png";

import Footer from "../components/layout/Footer";

import styles from "./CheckoutLayout.module.css";

function CheckoutLayout() {
  return (
    <div className={styles.app}>
      <div className={styles.topBar}>
        <div className={styles.topContainer}>
          <span>Kênh Người Bán</span>
          <span>|</span>
          <span>Tải ứng dụng</span>
          <span>|</span>
          <span>Kết nối</span>

          <div className={styles.topRight}>
            <span>Thông Báo</span>
            <span>Hỗ Trợ</span>
            <span>Tiếng Việt</span>
          </div>
        </div>
      </div>

      <header className={styles.checkoutHeader}>
        <div className={styles.headerContainer}>
          <Link to="/" className={styles.logoLink}>
            <img
              src={shopeeCheckoutLogo}
              alt="Shopee"
              className={styles.logo}
            />
          </Link>

          <span className={styles.divider} />

          <span className={styles.title}>
            Thanh Toán
          </span>
        </div>
      </header>

      <div className={styles.content}>
        <Outlet />
      </div>

      <Footer />
    </div>
  );
}

export default CheckoutLayout;