import { Link, useLocation, useNavigate } from "react-router-dom";

import {useEffect, useState, } from "react";

import useAuth from "../../features/auth/hooks/useAuth";

import styles from "./Header.module.css";

import shopeeLogo from "../../assets/images/logo/shopee-logo.png";

function Header() {
  const {
    user,
    isAuthenticated,
    logout,
  } = useAuth();

  const navigate = useNavigate();
  const location = useLocation();
  const isHomePage = location.pathname === "/";

  const [searchKeyword, setSearchKeyword] = useState("");

  useEffect(() => {
    if (location.pathname !== "/products") {
      return;
    }

    const searchParams = new URLSearchParams(
      location.search,
    );

    setSearchKeyword(
      searchParams.get("keyword") || "",
    );
  }, [location.pathname, location.search]);



  function handleLogout() {
    logout();
    navigate("/login");
  }

  function handleSearchSubmit(event) {
    event.preventDefault();

    const keyword = searchKeyword.trim();

    const nextSearchParams = new URLSearchParams();

    if (keyword) {
      nextSearchParams.set("keyword", keyword);
    }

    nextSearchParams.set("page", "1");

    navigate(
      `/products?${nextSearchParams.toString()}`,
    );
  }

  return (
    <header
      className={`${styles.header} ${
        location.pathname === "/"
          ? styles.stickyHeader
          : ""
      }`}
    >
      <div className={styles.topBar}>
        <div className={styles.container}>
          <nav className={styles.topNavigation}>
            <Link to="/seller">
              Kênh Người Bán
            </Link>

            <span className={styles.separator} />

            <Link to="/shops/register">
              Trở thành Người bán Shopee
            </Link>

            <span className={styles.separator} />

            <button type="button">
              Tải ứng dụng
            </button>

            <span className={styles.separator} />

            <span>Kết nối</span>
          </nav>

          <nav className={styles.accountNavigation}>
            <button type="button">
              Thông Báo
            </button>

            <button type="button">
              Hỗ Trợ
            </button>

            <button type="button">
              Tiếng Việt
            </button>

            {isAuthenticated ? (
              <div className={styles.userMenu}>
                <Link to="/profile">
                  {user?.fullName || user?.email}
                </Link>

                <button
                  type="button"
                  onClick={handleLogout}
                >
                  Đăng xuất
                </button>
              </div>
            ) : (
              <>
                <Link to="/register">
                  Đăng Ký
                </Link>

                <span className={styles.separator} />

                <Link to="/login">
                  Đăng Nhập
                </Link>
              </>
            )}
          </nav>
        </div>
      </div>

      <div className={styles.mainHeader}>
        <div className={styles.container}>
          <Link
            to="/"
            className={styles.logo}
            aria-label="Shopee - Trang chủ"
          >
            <img
                src={shopeeLogo}
                alt="Shopee"
                className={styles.logoImage}
            />
          </Link>

          <div className={styles.searchArea}>
            <form
              className={styles.searchForm}
              onSubmit={handleSearchSubmit}
            >
              <input
                name="keyword"
                type="search"
                value={searchKeyword}
                onChange={(event) =>
                  setSearchKeyword(event.target.value)
                }
                placeholder="Tìm kiếm sản phẩm, thương hiệu và tên shop"
                aria-label="Tìm kiếm sản phẩm"
              />

              <button type="submit">
                Tìm kiếm
              </button>
            </form>

            <div className={styles.suggestions}>
              <Link to="/products?keyword=điện%20thoại&page=1">
                Điện thoại
              </Link>

              <Link to="/products?keyword=áo&page=1">
                Áo thời trang
              </Link>

              <Link to="/products?keyword=tai%20nghe&page=1">
                Tai nghe
              </Link>

              <Link to="/products?keyword=phụ kiện">
                Phụ kiện
              </Link>

              <Link to="/products?keyword=mỹ phẩm">
                Mỹ phẩm
              </Link>
            </div>
          </div>

          <Link
            to="/cart"
            className={styles.cart}
            aria-label="Giỏ hàng"
          >
            🛒
          </Link>
        </div>
      </div>
    </header>
  );
}

export default Header;