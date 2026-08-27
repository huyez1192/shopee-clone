import {useCallback, useEffect, useMemo, useState, } from "react";

import { Link } from "react-router-dom";

import fallbackProductImage from "../../../../assets/images/product-placeholder.png";

import {
  completeSellerOrder,
  confirmSellerOrder,
  getSellerOrders,
  shipSellerOrder,
} from "../api/sellerOrderApi";

import {filterSellerOrders, getSellerOrderStatusLabel, SELLER_ORDER_TABS,} from "../utils/sellerOrderUtils";

import styles from "./SellerOrderListPage.module.css";

function formatPrice(value) {
  return new Intl.NumberFormat("vi-VN").format(
    Number(value) || 0,
  );
}

function getPaymentLabel(order) {
  if (order.paymentMethod === "COD") {
    return order.paymentStatus === "PAID"
      ? "COD - Đã thanh toán"
      : "COD - Chưa thanh toán";
  }

  if (order.paymentMethod === "SEPAY") {
    return order.paymentStatus === "PAID"
      ? "SePay - Đã thanh toán"
      : "SePay - Chưa thanh toán";
  }

  return order.paymentMethod;
}

function SellerOrderListPage() {
  const [orders, setOrders] = useState([]);
  const [activeTab, setActiveTab] = useState("all");

  const [keyword, setKeyword] = useState("");

  const [isLoading, setIsLoading] = useState(true);

  const [errorMessage, setErrorMessage] = useState("");

  const [processingOrderId, setProcessingOrderId] = useState(null);

  const loadOrders = useCallback(async () => {
    try {
      setIsLoading(true);
      setErrorMessage("");

      const response = await getSellerOrders();

      setOrders(
        Array.isArray(response)
          ? response
          : response?.content || [],
      );
    } catch (error) {
      console.error(error);

      setErrorMessage(
        error?.response?.data?.message ||
          "Không thể tải danh sách đơn hàng.",
      );
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadOrders();
  }, [loadOrders]);

  const filteredOrders = useMemo(() => {
    const tabOrders =
      filterSellerOrders(
        orders,
        activeTab,
      );

    const searchValue = keyword.trim().toLowerCase();

    if (!searchValue) {
      return tabOrders;
    }

    return tabOrders.filter((order) => {
      const orderCode =
        order.orderCode
          ?.toLowerCase()
          .includes(searchValue);

      const buyerName =
        order.buyerName
          ?.toLowerCase()
          .includes(searchValue);

      const productName =
        order.items?.some((item) =>
          item.productName
            ?.toLowerCase()
            .includes(searchValue),
        );

      return (
        orderCode ||
        buyerName ||
        productName
      );
    });
  }, [
    orders,
    activeTab,
    keyword,
  ]);

  async function handleAction(
    order,
    action,
  ) {
    try {
      setProcessingOrderId(order.id);

      if (action === "confirm") {
        await confirmSellerOrder(order.id);
      }

      if (action === "ship") {
        await shipSellerOrder(order.id);
      }

      if (action === "complete") {
        await completeSellerOrder(order.id);
      }

      await loadOrders();
    } catch (error) {
      alert(
        error?.response?.data?.message ||
          "Không thể cập nhật đơn hàng.",
      );
    } finally {
      setProcessingOrderId(null);
    }
  }

  function renderMainAction(order) {
    const processing =
      processingOrderId === order.id;

    if (
      order.status === "PENDING" &&
      order.paymentMethod === "SEPAY" &&
      order.paymentStatus !== "PAID"
    ) {
      return (
        <button
          type="button"
          disabled
          className={styles.disabledButton}
        >
          Chờ thanh toán
        </button>
      );
    }

    if (order.status === "PENDING") {
      return (
        <button
          type="button"
          disabled={processing}
          className={styles.primaryButton}
          onClick={() =>
            handleAction(
              order,
              "confirm",
            )
          }
        >
          {processing
            ? "Đang xử lý..."
            : "Xác nhận"}
        </button>
      );
    }

    if (order.status === "CONFIRMED") {
      return (
        <button
          type="button"
          disabled={processing}
          className={styles.primaryButton}
          onClick={() =>
            handleAction(
              order,
              "ship",
            )
          }
        >
          {processing
            ? "Đang xử lý..."
            : "Giao hàng"}
        </button>
      );
    }

    if (order.status === "SHIPPING") {
      return (
        <button
          type="button"
          disabled={processing}
          className={styles.primaryButton}
          onClick={() =>
            handleAction(
              order,
              "complete",
            )
          }
        >
          {processing
            ? "Đang xử lý..."
            : "Hoàn thành"}
        </button>
      );
    }

    return null;
  }

  if (isLoading) {
    return (
      <div className={styles.stateBox}>
        Đang tải đơn hàng...
      </div>
    );
  }

  if (errorMessage) {
    return (
      <div className={styles.stateBox}>
        <p>{errorMessage}</p>

        <button
          type="button"
          onClick={loadOrders}
          className={styles.retryButton}
        >
          Thử lại
        </button>
      </div>
    );
  }

  return (
    <section className={styles.page}>
      <div className={styles.pageHeader}>
        <div>
          <h1>Quản lý đơn hàng</h1>

          <p>
            Theo dõi và xử lý các đơn hàng
            của shop.
          </p>
        </div>
      </div>

      <div className={styles.tabs}>
        {SELLER_ORDER_TABS.map(
          (tab) => (
            <button
              key={tab.value}
              type="button"
              className={
                activeTab === tab.value
                  ? styles.activeTab
                  : styles.tab
              }
              onClick={() =>
                setActiveTab(tab.value,)
              }
            >
              {tab.label}
            </button>
          ),
        )}
      </div>

      <div className={styles.toolbar}>
        <div className={styles.search}>
          <input
            type="text"
            value={keyword}
            placeholder="Tìm theo mã đơn, người mua, sản phẩm..."
            onChange={(event) =>
              setKeyword(
                event.target.value,
              )
            }
          />

          <span>⌕</span>
        </div>

        <div className={styles.dateGroup}>
          <span>Ngày đặt hàng</span>

          <input type="date" />

          <span>-</span>

          <input type="date" />
        </div>
      </div>

      <div className={styles.resultHeader}>
        <h2>
          {filteredOrders.length} Đơn hàng
        </h2>

        <button
          type="button"
          className={styles.refreshButton}
          onClick={loadOrders}
        >
          ↻ Làm mới
        </button>
      </div>

      <div className={styles.tableHeader}>
        <span>Sản phẩm</span>
        <span>Tổng đơn hàng</span>
        <span>Trạng thái</span>
        <span>Thanh toán</span>
        <span>Thao tác</span>
      </div>

      {filteredOrders.length === 0 ? (
        <div className={styles.empty}>
          <div className={styles.emptyIcon}>
            📦
          </div>

          <strong>
            Không có đơn hàng
          </strong>

          <span>
            Chưa có đơn phù hợp với bộ lọc hiện tại.
          </span>
        </div>
      ) : (
        <div className={styles.orderList}>
          {filteredOrders.map(
            (order) => (
              <article
                key={order.id}
                className={styles.orderCard}
              >
                <div className={styles.orderTop}>
                  <div className={styles.buyer}>
                    <div className={styles.avatar}>
                      👤
                    </div>

                    <div>
                      <strong>
                        {order.buyerName}
                      </strong>

                      <span>
                        {order.itemCount} sản phẩm
                      </span>
                    </div>
                  </div>

                  <div className={styles.orderCode}>
                    ID Đơn hàng{" "}
                    <strong>
                      {order.orderCode}
                    </strong>
                  </div>
                </div>

                <div className={styles.orderBody}>
                  <div className={styles.products}>
                    {order.items?.map(
                      (item) => {
                        const variant =
                          [
                            item.variantValue1,
                            item.variantValue2,
                          ]
                            .filter(Boolean)
                            .join(", ");

                        return (
                          <div
                            key={item.id}
                            className={styles.product}
                          >
                            <img
                              src={item.productImageUrl || fallbackProductImage}
                              alt={item.productName}
                              onError={(event,) => {
                                event.currentTarget.src = fallbackProductImage;
                              }}
                            />

                            <div className={styles.productInfo}>
                              <strong>
                                {item.productName}
                              </strong>

                              {variant && (
                                <span>
                                  Phân loại:{" "}
                                  {variant}
                                </span>
                              )}

                              <span>
                                x{item.quantity}
                              </span>
                            </div>
                          </div>
                        );
                      },
                    )}
                  </div>

                  <div className={styles.total}>
                    <strong>
                      ₫ {formatPrice(order.totalAmount,)}
                    </strong>

                    <span>
                      Tổng thanh toán
                    </span>
                  </div>

                  <div className={styles.status}>
                    <span
                      className={
                        order.status === "COMPLETED"
                          ? styles.statusCompleted
                          : order.status === "CANCELLED"
                            ? styles.statusCancelled
                            : styles.statusProcessing
                      }
                    >
                      {getSellerOrderStatusLabel(order,)}
                    </span>
                  </div>

                  <div className={styles.payment}>
                    <strong>
                      {getPaymentLabel(order,)}
                    </strong>
                  </div>

                  <div className={styles.actions}>
                    {renderMainAction(order,)}

                    <Link
                      to={`/seller/orders/${order.id}`}
                      className={styles.detailLink}
                    >
                      Xem chi tiết
                    </Link>
                  </div>
                </div>
              </article>
            ),
          )}
        </div>
      )}
    </section>
  );
}

export default SellerOrderListPage;