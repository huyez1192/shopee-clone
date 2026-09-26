import { Link, NavLink, Outlet } from "react-router-dom";

import styles from "./SellerLayout.module.css";

import shopeeCheckoutLogo from "../assets/images/logo/shopee-checkout-logo.png";

function SellerLayout() {
  return (
    <div className={styles.layout}>
      <header className={styles.header}>
        <div className={styles.brand}>
          <Link to="/" className={styles.logoLink} aria-label="Về trang chủ Shopee">
            <img src={shopeeCheckoutLogo} alt="Shopee" className={styles.logo} />
          </Link>
          <Link to="/seller" className={styles.sellerHome}>Kênh Người Bán</Link>
        </div>

        <div className={styles.headerRight}>
          <Link to="/notifications" aria-label="Xem thông báo">🔔</Link>
          <Link to="/">Về trang mua sắm</Link>
        </div>
      </header>

      <div className={styles.body}>
        <aside className={styles.sidebar}>
          <div className={styles.menuGroup}>
            <strong>▣ Quản Lý Đơn hàng</strong>

            <NavLink to="/seller/orders" className={({ isActive }) => isActive ? styles.activeMenu : undefined}>
              Tất cả
            </NavLink>

            <span>Xử Lý Đơn Hàng Loạt</span>

            <span className={styles.disabled}>
              Trả hàng / Hoàn tiền
            </span>
          </div>

          <div className={styles.menuGroup}>
            <strong>▱ Quản Lý Sản Phẩm</strong>

            <NavLink to="/seller/products" end className={({ isActive }) => isActive ? styles.activeMenu : undefined}>
              Tất Cả Sản Phẩm
            </NavLink>

            <NavLink to="/seller/products/create" className={({ isActive }) => isActive ? styles.activeMenu : undefined}>
              Thêm Sản Phẩm
            </NavLink>

            <NavLink to="/seller/inventory" className={({ isActive }) => isActive ? styles.activeMenu : undefined}>
                Quản Lý Tồn Kho
            </NavLink>
          </div>

          <div className={styles.menuGroup}>
            <strong>★ Quản Lý Đánh Giá</strong>

            <NavLink to="/seller/reviews" className={({ isActive }) => isActive ? styles.activeMenu : undefined}>
              Đánh Giá Shop
            </NavLink>
          </div>

          <div className={styles.menuGroup}>
            <strong>✉ Chăm Sóc Khách Hàng</strong>
            <NavLink
              to="/seller/chat"
              className={({ isActive }) => isActive ? styles.activeMenu : undefined}
            >
              Tin nhắn
            </NavLink>
          </div>

          <div className={styles.menuGroup}>
            <strong>▱ Tài chính</strong>

            <NavLink
              to="/seller"
              end
              className={({ isActive }) => isActive ? styles.activeMenu : undefined}
            >
              Doanh thu
            </NavLink>
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
