import {useCallback, useEffect, useMemo, useState,} from "react";
import { Link } from "react-router-dom";

import EmptyState from "../../../components/common/EmptyState";
import ErrorState from "../../../components/common/ErrorState";
import LoadingState from "../../../components/common/LoadingState";

import { getApiErrorMessage } from "../../../utils/getApiErrorMessage";

import { getOrders } from "../api/orderApi";

import OrderCard from "../components/OrderCard";

import {filterOrdersByTab, ORDER_TABS,} from "../utils/orderListUtils";

import styles from "./OrderListPage.module.css";

function OrderListPage() {
  const [orders, setOrders] = useState([]);

  const [activeTab, setActiveTab] =
    useState("all");

  const [isLoading, setIsLoading] =
    useState(true);

  const [errorMessage, setErrorMessage] =
    useState("");

  const loadOrders = useCallback(async () => {
    try {
      setIsLoading(true);
      setErrorMessage("");

      const response = await getOrders();

      setOrders(
        Array.isArray(response)
          ? response
          : [],
      );
    } catch (error) {
      setErrorMessage(
        getApiErrorMessage(
          error,
          "Không thể tải danh sách đơn hàng.",
        ),
      );
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadOrders();
  }, [loadOrders]);

  const filteredOrders = useMemo(
    () =>
      filterOrdersByTab(
        orders,
        activeTab,
      ),
    [
      orders,
      activeTab,
    ],
  );

  if (isLoading) {
    return (
      <LoadingState message="Đang tải đơn hàng..." />
    );
  }

  if (errorMessage) {
    return (
      <ErrorState
        message={errorMessage}
        onRetry={loadOrders}
      />
    );
  }

  return (
    <main className={styles.page}>
      <div className={styles.container}>
        <aside className={styles.sidebar}>
          <div className={styles.profile}>
            <div className={styles.avatar}>
              👤
            </div>

            <div>
              <strong>Tài khoản của tôi</strong>

              <span>Quản lý tài khoản</span>
            </div>
          </div>

          <nav className={styles.sideNav}>
            <Link to="/profile">
              👤 Tài Khoản Của Tôi
            </Link>

            <Link to="/addresses">
              📍 Địa Chỉ
            </Link>

            <Link
              to="/orders"
              className={styles.sideActive}
            >
              📋 Đơn Mua
            </Link>
          </nav>
        </aside>

        <div className={styles.content}>
          <div className={styles.tabs}>
            {ORDER_TABS.map((tab) => (
              <button
                key={tab.value}
                type="button"
                className={
                  activeTab === tab.value
                    ? styles.activeTab
                    : styles.tab
                }
                onClick={() =>
                  setActiveTab(tab.value)
                }
              >
                {tab.label}
              </button>
            ))}
          </div>

          {filteredOrders.length === 0 ? (
            <div className={styles.emptyWrapper}>
              <EmptyState
                title="Chưa có đơn hàng"
                description="Không có đơn hàng trong trạng thái này."
              />
            </div>
          ) : (
            <div className={styles.orders}>
              {filteredOrders.map(
                (order) => (
                  <OrderCard
                    key={order.id}
                    order={order}
                  />
                ),
              )}
            </div>
          )}
        </div>
      </div>
    </main>
  );
}

export default OrderListPage;