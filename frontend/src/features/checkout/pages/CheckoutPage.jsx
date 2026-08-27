import {useCallback,useEffect,useState,} from "react";

import {Link,useLocation,useNavigate,} from "react-router-dom";

import EmptyState from "../../../components/common/EmptyState";
import ErrorState from "../../../components/common/ErrorState";
import LoadingState from "../../../components/common/LoadingState";

import { getAddresses } from "../../address/api/addressApi";
import { createOrders } from "../../order/api/orderApi";

import { getCheckoutPreview } from "../api/checkoutApi";

import { getApiErrorMessage } from "../../../utils/getApiErrorMessage";

import styles from "./CheckoutPage.module.css";

function formatPrice(price) {
  return new Intl.NumberFormat("vi-VN").format(
    Number(price) || 0,
  );
}

function CheckoutPage() {
  const location = useLocation();
  const navigate = useNavigate();

  const cartItemIds = location.state?.cartItemIds || [];

  const [addresses, setAddresses] = useState([]);

  const [selectedAddressId, setSelectedAddressId] = useState(null);

  const [checkoutData, setCheckoutData] = useState(null);

  const [isLoading, setIsLoading] = useState(true);

  const [isPlacingOrder, setIsPlacingOrder] = useState(false);

  const [errorMessage, setErrorMessage] = useState("");

  const [paymentMethod, setPaymentMethod] = useState("COD");

  const loadCheckout = useCallback(async () => {
    if (cartItemIds.length === 0) {
      setIsLoading(false);
      return;
    }

    try {
      setIsLoading(true);
      setErrorMessage("");

      const addressResponse =
        await getAddresses();

      const addressList =
        Array.isArray(addressResponse)
          ? addressResponse
          : [];

      setAddresses(addressList);

      if (addressList.length === 0) {
        setCheckoutData(null);
        return;
      }

      const defaultAddress =
        addressList.find(
          (address) => address.defaultAddress,
        ) || addressList[0];

      setSelectedAddressId(defaultAddress.id);

      const preview =
        await getCheckoutPreview({
          addressId: defaultAddress.id,
          cartItemIds,
        });

      setCheckoutData(preview);
    } catch (error) {
      setErrorMessage(
        getApiErrorMessage(
          error,
          "Không thể tải thông tin thanh toán.",
        ),
      );
    } finally {
      setIsLoading(false);
    }
  }, [cartItemIds]);

  useEffect(() => {
    loadCheckout();
  }, [loadCheckout]);

  async function handleAddressChange(
    addressId,
  ) {
    try {
      setErrorMessage("");

      setSelectedAddressId(addressId);

      const preview =
        await getCheckoutPreview({
          addressId,
          cartItemIds,
        });

      setCheckoutData(preview);
    } catch (error) {
      setErrorMessage(
        getApiErrorMessage(
          error,
          "Không thể cập nhật địa chỉ nhận hàng.",
        ),
      );
    }
  }

  async function handlePlaceOrder() {
    if (
      !selectedAddressId ||
      cartItemIds.length === 0
    ) {
      return;
    }

    try {
      setIsPlacingOrder(true);
      setErrorMessage("");

      const response = await createOrders({
        addressId: selectedAddressId,
        cartItemIds,
        paymentMethod,
      });

      if (paymentMethod === "SEPAY") {
        if (!response.payment) {
          throw new Error(
            "Backend không trả thông tin thanh toán SePay.",
          );
        }

        sessionStorage.setItem(
          "sepayPayment",
          JSON.stringify(response.payment),
        );

        navigate("/payment/sepay", {
          replace: true,
          state: {
            payment: response.payment,
          },
        });

        return;
      }

      navigate("/orders", {
        replace: true,
        state: {
          createdOrders: response.orders,
        },
      });
    } catch (error) {
      setErrorMessage(
        getApiErrorMessage(
          error,
          "Không thể đặt hàng.",
        ),
      );
    } finally {
      setIsPlacingOrder(false);
    }
  }

  if (isLoading) {
    return (
      <LoadingState message="Đang chuẩn bị đơn hàng..." />
    );
  }

  if (cartItemIds.length === 0) {
    return (
      <EmptyState
        title="Chưa có sản phẩm để thanh toán"
        description="Hãy chọn sản phẩm trong giỏ hàng trước."
      />
    );
  }

  if (
    addresses.length === 0 &&
    !errorMessage
  ) {
    return (
      <main>
        <h1>Thanh toán</h1>

        <p>
          Bạn chưa có địa chỉ nhận hàng.
        </p>

        <Link to="/addresses">
          Thêm địa chỉ nhận hàng
        </Link>
      </main>
    );
  }

  if (errorMessage && !checkoutData) {
    return (
      <ErrorState
        message={errorMessage}
        onRetry={loadCheckout}
      />
    );
  }

  if (!checkoutData) {
    return (
      <EmptyState
        title="Không thể thanh toán"
        description="Không có dữ liệu checkout."
      />
    );
  }

  return (
    <main className={styles.page}>
      <div className={styles.container}>
        {errorMessage && (
          <div
            className={styles.errorMessage}
            role="alert"
          >
            {errorMessage}
          </div>
        )}

        <section className={styles.addressSection}>
          <div className={styles.addressBorder} />

          <h2 className={styles.addressTitle}>
            📍 Địa Chỉ Nhận Hàng
          </h2>

          {addresses.map((address) => (
            <label
              key={address.id}
              className={
                selectedAddressId === address.id
                  ? styles.addressActive
                  : styles.address
              }
            >
              <input
                type="radio"
                name="address"
                checked={
                  selectedAddressId === address.id
                }
                onChange={() =>
                  handleAddressChange(address.id)
                }
              />

              <div className={styles.addressContent}>
                <div className={styles.receiver}>
                  <strong>
                    {address.recipientName}
                  </strong>

                  <strong>
                    {address.recipientPhone}
                  </strong>
                </div>

                <span className={styles.addressText}>
                  {address.detailAddress},{" "}
                  {address.ward},{" "}
                  {address.province}
                </span>

                {address.defaultAddress && (
                  <span className={styles.defaultBadge}>
                    Mặc Định
                  </span>
                )}
              </div>
            </label>
          ))}
        </section>

        <section className={styles.productSection}>
          <div className={styles.productHeader}>
            <span>Sản phẩm</span>
            <span>Đơn giá</span>
            <span>Số lượng</span>
            <span>Thành tiền</span>
          </div>

          {checkoutData.shopGroups.map((shop) => (
            <div
              key={shop.shopId}
              className={styles.shopGroup}
            >
              <div className={styles.shopHeader}>
                <strong>{shop.shopName}</strong>
              </div>

              {shop.items.map((item) => {
                const variantText = [
                  item.variantValue1,
                  item.variantValue2,
                ]
                  .filter(Boolean)
                  .join(", ");

                return (
                  <div
                    key={item.cartItemId}
                    className={styles.productRow}
                  >
                    <div className={styles.productInfo}>
                      <img
                        src={item.productImageUrl}
                        alt={item.productName}
                        className={styles.productImage}
                      />

                      <div>
                        <div className={styles.productName}>
                          {item.productName}
                        </div>

                        {variantText && (
                          <div className={styles.variant}>
                            Phân loại hàng:{" "}
                            {variantText}
                          </div>
                        )}
                      </div>
                    </div>

                    <div className={styles.price}>
                      ₫{formatPrice(item.unitPrice)}
                    </div>

                    <div className={styles.quantity}>
                      {item.quantity}
                    </div>

                    <div className={styles.lineTotal}>
                      ₫{formatPrice(item.lineTotal)}
                    </div>
                  </div>
                );
              })}

              <div className={styles.shippingRow}>
                <div>
                  <strong>
                    Phương thức vận chuyển:
                  </strong>

                  <span className={styles.shippingMethod}>
                    Nhanh
                  </span>
                </div>

                <span>
                  ₫{formatPrice(shop.shippingFee)}
                </span>
              </div>

              <div className={styles.shopTotal}>
                <span>
                  Tổng số tiền (
                  {shop.items.length} sản phẩm):
                </span>

                <strong>
                  ₫{formatPrice(shop.shopTotal)}
                </strong>
              </div>
            </div>
          ))}
        </section>

        <section className={styles.paymentSection}>
          <div className={styles.paymentHeader}>
            <h2>Phương thức thanh toán</h2>

            <div className={styles.paymentMethods}>
              <button
                type="button"
                className={
                  paymentMethod === "COD"
                    ? styles.paymentActive
                    : styles.paymentOption
                }
                onClick={() =>
                  setPaymentMethod("COD")
                }
              >
                Thanh toán khi nhận hàng
              </button>

              <button
                type="button"
                className={
                  paymentMethod === "SEPAY"
                    ? styles.paymentActive
                    : styles.paymentOption
                }
                onClick={() =>
                  setPaymentMethod("SEPAY")
                }
              >
                Chuyển khoản QR - SePay
              </button>
            </div>
          </div>

          <div className={styles.paymentDescription}>
            {paymentMethod === "COD" ? (
              <>
                <span>
                  Thanh toán khi nhận hàng (COD)
                </span>

                <span>
                  Thanh toán cho đơn vị vận chuyển
                  khi nhận được hàng.
                </span>
              </>
            ) : (
              <>
                <span>
                  Chuyển khoản ngân hàng qua QR
                </span>

                <span>
                  Quét mã QR để thanh toán toàn bộ
                  đơn hàng qua SePay.
                </span>
              </>
            )}
          </div>

          <div className={styles.summary}>
            <div className={styles.summaryRow}>
              <span>Tổng tiền hàng</span>

              <span>
                ₫
                {formatPrice(
                  checkoutData.merchandiseTotal,
                )}
              </span>
            </div>

            <div className={styles.summaryRow}>
              <span>Tổng tiền phí vận chuyển</span>

              <span>
                ₫
                {formatPrice(
                  checkoutData.shippingTotal,
                )}
              </span>
            </div>

            <div className={styles.summaryTotal}>
              <span>Tổng thanh toán</span>

              <strong>
                ₫
                {formatPrice(
                  checkoutData.grandTotal,
                )}
              </strong>
            </div>
          </div>

          <div className={styles.orderFooter}>
            <p>
              Nhấn "Đặt hàng" đồng nghĩa với việc
              bạn đồng ý với điều khoản Shopee.
            </p>

            <button
              type="button"
              className={styles.orderButton}
              disabled={isPlacingOrder}
              onClick={handlePlaceOrder}
            >
              {isPlacingOrder
                ? "Đang đặt hàng..."
                : "Đặt hàng"}
            </button>
          </div>
        </section>
      </div>
    </main>
  );
}

export default CheckoutPage;