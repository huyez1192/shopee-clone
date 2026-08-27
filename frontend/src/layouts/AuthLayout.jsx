import { Link, Outlet, useLocation } from "react-router-dom";

import shopeeCheckoutLogo from "../assets/images/logo/shopee-checkout-logo.png";

import Footer from "../components/layout/Footer";

import styles from "./AuthLayout.module.css";

import shopeeBigLogo from "../assets/images/logo/shopee-big-logo.png";

function AuthLayout() {
  const location = useLocation();

  const isLogin = location.pathname === "/login";

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div className={styles.headerContainer}>
          <Link
            to="/"
            className={styles.brand}
          >
            <img
              src={shopeeCheckoutLogo}
              alt="Shopee"
            />

            <span>
              {isLogin
                ? "Đăng nhập"
                : "Đăng ký"}
            </span>
          </Link>

          <Link
            to="/"
            className={styles.help}
          >
            Bạn cần giúp đỡ?
          </Link>
        </div>
      </header>

      <main className={styles.hero}>
        <div className={styles.heroContainer}>
          <img
              src={shopeeBigLogo}
              alt="Shopee"
              className={styles.logoImage}
          />

          <div className={styles.formArea}>
            <Outlet />
          </div>
        </div>
      </main>

      <Footer />
    </div>
  );
}

export default AuthLayout;