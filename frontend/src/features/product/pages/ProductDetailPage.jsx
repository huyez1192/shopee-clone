import {useCallback, useEffect, useMemo, useState,} from "react";

import {Link, useLocation, useNavigate, useParams,} from "react-router-dom";

import EmptyState from "../../../components/common/EmptyState";
import ErrorState from "../../../components/common/ErrorState";
import LoadingState from "../../../components/common/LoadingState";

import fallbackProductImage from "../../../assets/images/product-placeholder.png";

import { getApiErrorMessage } from "../../../utils/getApiErrorMessage";

import {getProductById, getProductVariants,} from "../api/productApi";

import styles from "./ProductDetailPage.module.css";

import useAuth from "../../auth/hooks/useAuth";

import { addCartItem } from "../../cart/api/cartApi";

function formatPrice(price) {
  if (price == null) {
    return "";
  }

  return new Intl.NumberFormat("vi-VN").format(price);
}

function ProductDetailPage() {
  const { productId } = useParams();

  const [product, setProduct] = useState(null);
  const [variants, setVariants] = useState([]);

  const [selectedValues, setSelectedValues] = useState({});

  const [quantity, setQuantity] = useState(1);

  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");

  const [showVariantError, setShowVariantError] = useState(false);

  const { isAuthenticated } = useAuth();

  const navigate = useNavigate();
  const location = useLocation();

  const [isAddingToCart, setIsAddingToCart] = useState(false);

  const [isBuyingNow, setIsBuyingNow] = useState(false);

  const [actionError, setActionError] = useState("");

  const [successMessage, setSuccessMessage] = useState("");


  const loadProduct = useCallback(async () => {
    try {
      setIsLoading(true);
      setErrorMessage("");

      const [
        productResponse,
        variantResponse,
      ] = await Promise.all([
        getProductById(productId),
        getProductVariants(productId),
      ]);

      setProduct(productResponse);

      setVariants(
        Array.isArray(variantResponse)
          ? variantResponse
          : [],
      );
    } catch (error) {
      setErrorMessage(
        getApiErrorMessage(
          error,
          "Không thể tải thông tin sản phẩm.",
        ),
      );
    } finally {
      setIsLoading(false);
    }
  }, [productId]);

  useEffect(() => {
    loadProduct();
  }, [loadProduct]);

  const variantGroups = useMemo(() => {
    const groups = {};

    variants.forEach((variant) => {
      if (
        variant.variantName1 &&
        variant.variantValue1
      ) {
        if (!groups[variant.variantName1]) {
          groups[variant.variantName1] = new Set();
        }

        groups[variant.variantName1].add(
          variant.variantValue1,
        );
      }

      if (
        variant.variantName2 &&
        variant.variantValue2
      ) {
        if (!groups[variant.variantName2]) {
          groups[variant.variantName2] = new Set();
        }

        groups[variant.variantName2].add(
          variant.variantValue2,
        );
      }
    });

    return Object.entries(groups).map(
      ([name, values]) => ({
        name,
        values: [...values],
      }),
    );
  }, [variants]);

  const selectedVariant = useMemo(() => {
    if (variantGroups.length === 0) {
      return variants.length === 1
        ? variants[0]
        : null;
    }

    return (
      variants.find((variant) => {
        const firstMatches =
          !variant.variantName1 ||
          selectedValues[variant.variantName1] ===
            variant.variantValue1;

        const secondMatches =
          !variant.variantName2 ||
          selectedValues[variant.variantName2] ===
            variant.variantValue2;

        return firstMatches && secondMatches;
      }) || null
    );
  }, [
    variants,
    variantGroups,
    selectedValues,
  ]);

  const hasVariants = variantGroups.length > 0;

  const isSelectionComplete =
    !hasVariants ||
    variantGroups.every(
      (group) => selectedValues[group.name],
    );

  const isInvalidCombination =
    hasVariants &&
    isSelectionComplete &&
    !selectedVariant;






  const displayedPrice =
    selectedVariant?.price ?? product?.minPrice;

  const availableStock =
    selectedVariant?.stockQuantity ??
    product?.totalStock ??
    0;

  const isOutOfStock = availableStock <= 0;

  function handleVariantSelect(groupName, value) {
    setSelectedValues((currentValues) => ({
      ...currentValues,
      [groupName]: value,
    }));

    setQuantity(1);
    setShowVariantError(false);
  }

  function handleDecreaseQuantity() {
    setQuantity((currentQuantity) =>
      Math.max(1, currentQuantity - 1),
    );
  }

  function handleIncreaseQuantity() {
    setQuantity((currentQuantity) =>
      Math.min(
        availableStock,
        currentQuantity + 1,
      ),
    );
  }

  function handleQuantityChange(event) {
    const nextQuantity = Number(
      event.target.value,
    );

    if (!Number.isInteger(nextQuantity)) {
      return;
    }

    setQuantity(
      Math.min(
        Math.max(1, nextQuantity),
        availableStock,
      ),
    );
  }

  if (isLoading) {
    return (
      <LoadingState message="Đang tải sản phẩm..." />
    );
  }

  if (errorMessage) {
    return (
      <ErrorState
        message={errorMessage}
        onRetry={loadProduct}
      />
    );
  }

  if (!product) {
    return (
      <EmptyState
        title="Không tìm thấy sản phẩm"
        description="Sản phẩm có thể đã bị xóa hoặc không còn hiển thị."
      />
    );
  }

  function validateVariantSelection() {
    if (hasVariants && !isSelectionComplete) {
      setShowVariantError(true);
      return false;
    }

    if (isInvalidCombination) {
      setShowVariantError(true);
      return false;
    }

    if (!selectedVariant) {
      setShowVariantError(true);
      return false;
    }

    setShowVariantError(false);

    return true;
  }

  async function handleBuyNow() {
    setActionError("");
    setSuccessMessage("");

    if (!validateVariantSelection()) {
      return;
    }

    if (!isAuthenticated) {
      const redirectPath =
        `${location.pathname}${location.search}`;

      navigate("/login", {
        state: {
          redirectPath,
        },
      });

      return;
    }

    try {
      setIsBuyingNow(true);

      const response = await addCartItem({
        variantId: selectedVariant.id,
        quantity,
      });

      /*
       * Tùy API addCartItem đang trả về:
       * - trực tiếp CartItem
       * - hoặc Cart có items
       */
      const cartItemId =
        response?.id ??
        response?.item?.id ??
        response?.items?.find(
          (item) =>
            Number(item.variantId) ===
            Number(selectedVariant.id),
        )?.id;

      if (!cartItemId) {
        throw new Error(
          "Không xác định được sản phẩm trong giỏ hàng.",
        );
      }

      navigate("/checkout", {
        state: {
          cartItemIds: [cartItemId],
        },
      });
    } catch (error) {
      setActionError(
        getApiErrorMessage(
          error,
          "Không thể mua sản phẩm ngay.",
        ),
      );
    } finally {
      setIsBuyingNow(false);
    }
  }



  async function handleAddToCart() {
    setActionError("");
    setSuccessMessage("");

    if (!validateVariantSelection()) {
      return;
    }

    if (!isAuthenticated) {
      const redirectPath =
        `${location.pathname}${location.search}`;

      navigate("/login", {
        state: {
          redirectPath,
        },
      });

      return;
    }

    try {
      setIsAddingToCart(true);

      await addCartItem({
        variantId: selectedVariant.id,
        quantity,
      });

      setSuccessMessage(
        "Đã thêm sản phẩm vào giỏ hàng.",
      );
    } catch (error) {
      setActionError(
        getApiErrorMessage(
          error,
          "Không thể thêm sản phẩm vào giỏ hàng.",
        ),
      );
    } finally {
      setIsAddingToCart(false);
    }
  }



  return (
    <main className={styles.page}>
      <div className={styles.container}>
        <section className={styles.mainProduct}>
          <div className={styles.gallery}>
            <div className={styles.mainImage}>
              <img
                src={
                  product.imageUrl ||
                  fallbackProductImage
                }
                alt={product.name}
                onError={(event) => {
                  event.currentTarget.src =
                    fallbackProductImage;
                }}
              />
            </div>

            <div className={styles.thumbnailList}>
              <button
                type="button"
                className={styles.thumbnailActive}
              >
                <img
                  src={
                    product.imageUrl ||
                    fallbackProductImage
                  }
                  alt={product.name}
                />
              </button>
            </div>

            <div className={styles.shareRow}>
              <span>Chia sẻ:</span>

              <span>🔵</span>
              <span>🔴</span>
              <span>⚫</span>

              <span className={styles.shareDivider} />

              <span>♡ Yêu thích</span>
            </div>
          </div>

          <div className={styles.productInfo}>
            <h1 className={styles.productName}>
              {product.name}
            </h1>

            <div className={styles.meta}>
              <span className={styles.ratingPlaceholder}>
                Chưa có đánh giá
              </span>

              <span className={styles.metaDivider} />

              <span>
                {product.variantCount - 1} phân loại
              </span>

              <span className={styles.metaDivider} />

              <span>
                Kho {product.totalStock}
              </span>
            </div>

            <div className={styles.priceBox}>
              {selectedVariant ? (
                <div className={styles.price}>
                  <span>₫</span>

                  {formatPrice(
                    selectedVariant.price,
                  )}
                </div>
              ) : product.minPrice ===
                product.maxPrice ? (
                <div className={styles.price}>
                  <span>₫</span>

                  {formatPrice(product.minPrice)}
                </div>
              ) : (
                <div className={styles.price}>
                  <span>₫</span>

                  {formatPrice(product.minPrice)}

                  <span className={styles.priceDash}>
                    -
                  </span>

                  <span>₫</span>

                  {formatPrice(product.maxPrice)}
                </div>
              )}
            </div>

            <div className={styles.purchaseOptions}>
              {variantGroups.map((group) => (
                <div
                  key={group.name}
                  className={styles.optionRow}
                >
                  <div className={styles.optionLabel}>
                    {group.name}
                  </div>

                  <div className={styles.optionValues}>
                    {group.values.map((value) => (
                      <button
                        key={value}
                        type="button"
                        className={
                          selectedValues[
                            group.name
                          ] === value
                            ? styles.variantActive
                            : styles.variantButton
                        }
                        onClick={() =>
                          handleVariantSelect(
                            group.name,
                            value,
                          )
                        }
                      >
                        {value}
                      </button>
                    ))}
                  </div>
                </div>
              ))}

              <div className={styles.optionRow}>
                <div className={styles.optionLabel}>
                  Số lượng
                </div>

                <div className={styles.quantityArea}>
                  <div className={styles.quantityControl}>
                    <button
                      type="button"
                      onClick={handleDecreaseQuantity}
                      disabled={
                        quantity <= 1 ||
                        isOutOfStock
                      }
                    >
                      −
                    </button>

                    <input
                      type="number"
                      min="1"
                      max={availableStock}
                      value={quantity}
                      onChange={handleQuantityChange}
                      disabled={isOutOfStock}
                    />

                    <button
                      type="button"
                      onClick={handleIncreaseQuantity}
                      disabled={
                        quantity >= availableStock ||
                        isOutOfStock
                      }
                    >
                      +
                    </button>
                  </div>

                  <span className={styles.stock}>
                    {isOutOfStock
                      ? "HẾT HÀNG"
                      : `${availableStock} sản phẩm có sẵn`}
                  </span>
                </div>
              </div>
            </div>

            <div className={styles.actions}>
              <button
                type="button"
                className={styles.addToCartButton}
                onClick={handleAddToCart}
                disabled={
                  Number(product.totalStock) <= 0 ||
                  isAddingToCart
                }
              >
                {isAddingToCart
                  ? "Đang thêm..."
                  : "🛒 Thêm Vào Giỏ Hàng"}
              </button>

              <button
                type="button"
                className={styles.buyNowButton}
                onClick={handleBuyNow}
                disabled={
                  Number(product.totalStock) <= 0 ||
                  isBuyingNow
                }
              >
                {isBuyingNow
                  ? "Đang xử lý..."
                  : "Mua Ngay"}
              </button>

              {successMessage && (
                <p className={styles.successMessage}>
                  {successMessage}
                </p>
              )}

              {actionError && (
                <p
                  className={styles.actionError}
                  role="alert"
                >
                  {actionError}
                </p>
              )}
            </div>

             {showVariantError &&
               !isSelectionComplete && (
                 <p className={styles.variantHint}>
                   Vui lòng chọn đầy đủ phân loại sản phẩm.
                 </p>
               )}

             {showVariantError &&
               isInvalidCombination && (
                 <p className={styles.variantHint}>
                   Phân loại sản phẩm này hiện không có sẵn.
                 </p>
               )}

             {isInvalidCombination && (
               <p className={styles.variantHint}>
                 Phân loại sản phẩm này hiện không có sẵn.
               </p>
             )}
          </div>
        </section>

        <section className={styles.shopSection}>
          <div className={styles.shopAvatar}>
            🏪
          </div>

          <div className={styles.shopMain}>
            <strong>
              {product.shopName}
            </strong>

            <span>
              Shop #{product.shopId}
            </span>
          </div>

          <Link
            to={`/shops/${product.shopId}`}
            className={styles.viewShopButton}
          >
            Xem Shop
          </Link>

          <div className={styles.shopStat}>
            <span>Sản phẩm</span>
            <strong>—</strong>
          </div>

          <div className={styles.shopStat}>
            <span>Đánh giá</span>
            <strong>—</strong>
          </div>
        </section>

        <section className={styles.detailSection}>
          <h2>CHI TIẾT SẢN PHẨM</h2>

          <div className={styles.detailRow}>
            <span>Danh mục</span>

            <strong>
              {product.categoryName}
            </strong>
          </div>

          <div className={styles.detailRow}>
            <span>Kho</span>

            <strong>
              {product.totalStock > 0
                ? "CÒN HÀNG"
                : "HẾT HÀNG"}
            </strong>
          </div>

          <div className={styles.detailRow}>
            <span>Shop</span>

            <strong>
              {product.shopName}
            </strong>
          </div>

          <h2 className={styles.descriptionTitle}>
            MÔ TẢ SẢN PHẨM
          </h2>

          <div className={styles.description}>
            {product.description ||
              "Sản phẩm chưa có mô tả."}
          </div>
        </section>
      </div>
    </main>
  );
}

export default ProductDetailPage;