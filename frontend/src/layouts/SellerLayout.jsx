import { Link, Outlet } from "react-router-dom";

import styles from "./SellerLayout.module.css";

import shopeeCheckoutLogo from "../assets/images/logo/shopee-checkout-logo.png";

function SellerLayout() {
  return (
    <div className={styles.layout}>
      <header className={styles.header}>
        <Link
          to="/seller/orders"
          className={styles.brand}
        >
          <Link to="/" className={styles.logoLink}>
              <img
                src={shopeeCheckoutLogo}
                alt="Shopee"
                className={styles.logo}
              />
            </Link>

          <span>Trang chủ / Đơn Bán</span>
        </Link>

        <div className={styles.headerRight}>
          <span>🔔</span>

          <button type="button">
            Trung Tâm Trợ Giúp
          </button>
        </div>
      </header>

      <div className={styles.body}>
        <aside className={styles.sidebar}>
          <div className={styles.menuGroup}>
            <strong>▣ Quản Lý Đơn hàng</strong>

            <Link to="/seller/orders">
              Tất cả
            </Link>

            <span>Xử Lý Đơn Hàng Loạt</span>

            <span className={styles.disabled}>
              Trả hàng / Hoàn tiền
            </span>
          </div>

          <div className={styles.menuGroup}>
            <strong>▱ Quản Lý Sản Phẩm</strong>

            <Link to="/seller/products">
              Tất Cả Sản Phẩm
            </Link>

            <Link to="/seller/products/create">
              Thêm Sản Phẩm
            </Link>

            <Link to="/seller/inventory">
                Quản Lý Tồn Kho
            </Link>
          </div>

          <div className={styles.menuGroup}>
            <strong>★ Quản Lý Đánh Giá</strong>

            <Link to="/seller/reviews">
              Đánh Giá Shop
            </Link>
          </div>

          <div className={styles.menuGroup}>
            <strong>▱ Tài chính</strong>

            <span>Doanh thu</span>
          </div>

          <div className={styles.menuGroup}>
            <strong>⌂ Quản Lý Shop</strong>

            <span>Thiết Lập Shop</span>
          </div>
        </aside>

        <main className={styles.content}>
          <Outlet />
        </main>
      </div>
    </div>
  );
}

export default SellerLayout;
