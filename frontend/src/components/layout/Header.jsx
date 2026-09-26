import { Link, useLocation, useNavigate } from "react-router-dom";

import { useCallback, useState } from "react";
import { useQuery } from "@tanstack/react-query";

import useAuth from "../../features/auth/hooks/useAuth";
import NotificationDropdown from "../../features/notification/components/NotificationDropdown";
import { getUnreadNotificationCount } from "../../features/notification/api/notificationApi";
import { queryKeys } from "../../query/queryKeys";
import { getCustomerUnreadCount } from "../../features/chat/api/chatApi";

import styles from "./Header.module.css";

import shopeeLogo from "../../assets/images/logo/shopee-logo.png";

function SearchForm({ initialKeyword }) {
  const navigate = useNavigate();
  const [searchKeyword, setSearchKeyword] = useState(initialKeyword);

  function handleSearchSubmit(event) {
    event.preventDefault();

    const keyword = searchKeyword.trim();
    const nextSearchParams = new URLSearchParams();

    if (keyword) {
      nextSearchParams.set("keyword", keyword);
    }

    nextSearchParams.set("page", "1");
    navigate(`/products?${nextSearchParams.toString()}`);
  }

  return (
    <form
      className={styles.searchForm}
      onSubmit={handleSearchSubmit}
    >
      <input
        name="keyword"
        type="search"
        value={searchKeyword}
        onChange={(event) => setSearchKeyword(event.target.value)}
        placeholder="Tìm kiếm sản phẩm, thương hiệu và tên shop"
        aria-label="Tìm kiếm sản phẩm"
      />

      <button type="submit">Tìm kiếm</button>
    </form>
  );
}

function Header() {
  const {
    user,
    isAuthenticated,
    logout,
  } = useAuth();

  const navigate = useNavigate();
  const [isNotificationOpen, setIsNotificationOpen] = useState(false);
  const closeNotifications = useCallback(() => setIsNotificationOpen(false), []);
  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);
  const initialKeyword = location.pathname === "/products"
    ? searchParams.get("keyword") || ""
    : "";

  const unreadCountQuery = useQuery({
    queryKey: queryKeys.notifications.unreadCount(),
    queryFn: getUnreadNotificationCount,
    enabled: isAuthenticated,
  });

  const unreadCount = Math.max(0, Number(unreadCountQuery.data?.count) || 0);
  const chatUnreadQuery = useQuery({
    queryKey: queryKeys.chat.customerUnread(),
    queryFn: getCustomerUnreadCount,
    enabled: isAuthenticated,
  });
  const chatUnread = Math.max(0, Number(chatUnreadQuery.data?.count) || 0);



  function handleLogout() {
    logout();
    navigate("/login");
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

            <span title="Chưa có trang đăng ký người bán trong frontend hiện tại">
              Trở thành Người bán Shopee
            </span>

            <span className={styles.separator} />

            <button type="button">
              Tải ứng dụng
            </button>

            <span className={styles.separator} />

            <span>Kết nối</span>
          </nav>

          <nav className={styles.accountNavigation}>
            {isAuthenticated && (
              <Link to="/chat" className={styles.chatLink}>
                Tin Nhắn
                {chatUnread > 0 && (
                  <span className={styles.chatBadge} aria-label={`${chatUnread} tin nhắn chưa đọc`}>
                    {chatUnread > 99 ? "99+" : chatUnread}
                  </span>
                )}
              </Link>
            )}
            <div className={styles.notificationArea}>
              <button
                type="button"
                className={styles.notificationTrigger}
                aria-expanded={isAuthenticated ? isNotificationOpen : undefined}
                aria-haspopup={isAuthenticated ? "dialog" : undefined}
                onClick={() => {
                  if (!isAuthenticated) {
                    navigate("/login", { state: { redirectPath: location.pathname } });
                    return;
                  }
                  setIsNotificationOpen((open) => !open);
                }}
              >
                <span className={styles.bellIcon} aria-hidden="true" />
                Thông Báo
                {unreadCount > 0 && (
                  <span className={styles.notificationBadge} aria-label={`${unreadCount} thông báo chưa đọc`}>
                    {unreadCount > 99 ? "99+" : unreadCount}
                  </span>
                )}
              </button>

              {isAuthenticated && isNotificationOpen && (
                <NotificationDropdown onClose={closeNotifications} />
              )}
            </div>

            <button type="button">
              Hỗ Trợ
            </button>

            <button type="button">
              Tiếng Việt
            </button>

            {isAuthenticated ? (
              <div className={styles.userMenu}>
                <span className={styles.userName}>
                  {user?.fullName || user?.email}
                </span>

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
            <SearchForm
              key={`${location.pathname}:${initialKeyword}`}
              initialKeyword={initialKeyword}
            />

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
