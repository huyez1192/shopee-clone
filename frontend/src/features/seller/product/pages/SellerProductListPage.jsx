import {useCallback, useEffect, useMemo, useState,} from "react";

import { Link } from "react-router-dom";

import fallbackProductImage from "../../../../assets/images/product-placeholder.png";

import {getSellerProducts, getSellerProductVariants,} from "../api/sellerProductApi";

import styles from "./SellerProductListPage.module.css";

function formatPrice(value) {
  return new Intl.NumberFormat(
    "vi-VN",
  ).format(Number(value) || 0);
}

function getVariantLabel(variant) {
  const values = [
    variant.variantValue1,
    variant.variantValue2,
  ].filter(Boolean);

  if (values.length === 0) {
    return "Mặc định";
  }

  return values.join(" / ");
}

function SellerProductListPage() {
  const [products, setProducts] = useState([]);

  const [keyword, setKeyword] = useState("");

  const [searchKeyword, setSearchKeyword] = useState("");

  const [isLoading, setIsLoading] = useState(true);

  const [errorMessage, setErrorMessage] = useState("");

  /*
   * variantsByProduct:
   *
   * {
   *   4101: [...],
   *   4102: [...]
   * }
   */
  const [variantsByProduct, setVariantsByProduct, ] = useState({});

  const [expandedProductIds, setExpandedProductIds, ] = useState([]);

  const [loadingVariantIds, setLoadingVariantIds, ] = useState([]);

  const loadProducts = useCallback(
    async () => {
      try {
        setIsLoading(true);
        setErrorMessage("");

        const response = await getSellerProducts();

        setProducts(
          Array.isArray(response)
            ? response
            : [],
        );
      } catch (error) {
        console.error(error);

        setErrorMessage(
          error?.response?.data?.message ||
            "Không thể tải danh sách sản phẩm.",
        );
      } finally {
        setIsLoading(false);
      }
    },
    [],
  );

  useEffect(() => {
    loadProducts();
  }, [loadProducts]);

  const filteredProducts = useMemo(() => {
    const value = searchKeyword.trim().toLowerCase();

    if (!value) {
      return products;
    }

    return products.filter(
      (product) =>
        product.name
          ?.toLowerCase()
          .includes(value) ||
        product.categoryName
          ?.toLowerCase()
          .includes(value),
    );
  }, [
    products,
    searchKeyword,
  ]);

  function handleSearch() {
    setSearchKeyword(keyword);
  }

  function handleReset() {
    setKeyword("");
    setSearchKeyword("");
  }

  async function handleToggleVariants(product,) {
    const isExpanded = expandedProductIds.includes(product.id,);

    if (isExpanded) {
      setExpandedProductIds(
        (current) =>
          current.filter(
            (id) =>
              id !== product.id,
          ),
      );

      return;
    }

    /*
     * Nếu đã tải variants trước đó
     * thì không gọi API lại.
     */
    if (variantsByProduct[product.id]) {
      setExpandedProductIds(
        (current) => [...current, product.id,],
      );

      return;
    }

    try {
      setLoadingVariantIds(
        (current) => [...current, product.id,],
      );

      const variants = await getSellerProductVariants(product.id,);

      setVariantsByProduct(
        (current) => ({
          ...current,

          [product.id]:
            Array.isArray(variants)
              ? variants
              : [],
        }),
      );

      setExpandedProductIds(
        (current) => [...current, product.id,],
      );
    } catch (error) {
      console.error(error);

      alert(
        error?.response?.data?.message || "Không thể tải phân loại sản phẩm.",
      );
    } finally {
      setLoadingVariantIds((current) =>
          current.filter((id) =>
              id !== product.id,
          ),
      );
    }
  }

  if (isLoading) {
    return (
      <div className={styles.state}>
        Đang tải sản phẩm...
      </div>
    );
  }

  if (errorMessage) {
    return (
      <div className={styles.state}>
        <p>{errorMessage}</p>

        <button
          type="button"
          onClick={loadProducts}
        >
          Thử lại
        </button>
      </div>
    );
  }

  return (
    <section className={styles.page}>
      {/* ================= HEADER ================= */}

      <div className={styles.pageHeader}>
        <div>
          <h1>Sản phẩm</h1>

          <p>
            Quản lý toàn bộ sản phẩm của shop
          </p>
        </div>

        <div
          className={styles.headerActions}
        >

          <Link
            to="/seller/products/create"
            className={styles.createButton}
          >
            + Thêm 1 sản phẩm mới
          </Link>
        </div>
      </div>

      {/* ================= SEARCH ================= */}

      <div className={styles.toolbar}>
        <div className={styles.searchBox}>
          <input
            type="text"
            value={keyword}
            placeholder="Tìm tên sản phẩm, ngành hàng..."
            onChange={(event) =>
              setKeyword(event.target.value,)
            }
            onKeyDown={(event) => {
              if (event.key === "Enter") {
                handleSearch();
              }
            }}
          />
        </div>

        <button
          type="button"
          className={styles.applyButton}
          onClick={handleSearch}
        >
          Áp dụng
        </button>

        <button
          type="button"
          className={styles.resetButton}
          onClick={handleReset}
        >
          Nhập lại
        </button>
      </div>

      {/* ================= COUNT ================= */}

      <div className={styles.summary}>
        <strong>
          {order.totalQuantity} Sản phẩm
        </strong>
      </div>

      {/* ================= TABLE ================= */}

      <div className={styles.table}>
        <div
          className={styles.tableHeader}
        >
          <div>Tên sản phẩm</div>

          <div>Giá</div>

          <div>Kho hàng</div>

          <div>Thao tác</div>
        </div>

        {filteredProducts.length ===
        0 ? (
          <div
            className={styles.emptyState}
          >
            Không có sản phẩm phù hợp.
          </div>
        ) : (
          filteredProducts.map((product) => {
              const expanded = expandedProductIds.includes(product.id,);

              const loadingVariants = loadingVariantIds.includes(product.id,);

              const variants =
                (variantsByProduct[product.id] || []).filter(
                  (variant) =>
                    variant.variantValue1 ||
                    variant.variantValue2,
                );

              const variantCount =
                product.variantCount ?? 0;


              return (
                <div
                  key={product.id}
                  className={styles.productBlock}
                >
                  {/* PRODUCT */}

                  <div
                    className={styles.productRow}
                  >
                    <div
                      className={styles.productInfo}
                    >
                      <img
                        src={item.productImageUrl || fallbackProductImage}
                        alt={item.productName}
                        className={styles.productImage}
                        onError={(event,) => {
                          event.currentTarget.src = fallbackProductImage;
                        }}
                      />

                      <div
                        className={styles.productMeta}
                      >
                        <h3>
                          {
                            product.name
                          }
                        </h3>

                        <p>
                          Ngành hàng:{" "}
                          {
                            product.categoryName
                          }
                        </p>

                        {variantCount > 0 && (
                          <button
                            type="button"
                            className={styles.variantToggle}
                            onClick={() =>
                              handleToggleVariants(product)
                            }
                          >
                            {loadingVariants
                              ? "Đang tải..."
                              : expanded
                                ? "Ẩn phân loại"
                                : `${variantCount} phân loại`}
                          </button>
                        )}
                      </div>
                    </div>

                    {/* PRICE */}

                    <div
                      className={styles.cell}
                    >
                      {product.minPrice ==
                      null ? (
                        "-"
                      ) : product.minPrice ===
                        product.maxPrice ? (
                        <span
                          className={styles.price}
                        >
                          ₫ {formatPrice(product.minPrice,)}
                        </span>
                      ) : (
                        <div
                          className={styles.priceRange}
                        >
                          <span>
                            ₫ {formatPrice(product.minPrice,)}
                          </span>

                          <span>
                            -
                          </span>

                          <span>
                            ₫ {formatPrice(product.maxPrice,)}
                          </span>
                        </div>
                      )}
                    </div>

                    {/* STOCK */}

                    <div className={styles.cell}>
                      {product.totalStock ??
                        0}
                    </div>

                    {/* ACTION */}

                    <div
                      className={styles.actionCell}
                    >
                      <Link to={`/seller/products/${product.id}/edit`}>
                        Cập nhật
                      </Link>

                      {variantCount > 0 && (
                        <button
                          type="button"
                          className={styles.variantToggle}
                          onClick={() =>
                            handleToggleVariants(product)
                          }
                        >
                          {loadingVariants
                            ? "Đang tải..."
                            : expanded
                              ? "Ẩn phân loại"
                              : `${variantCount} phân loại`}
                        </button>
                      )}

                      <button type="button">
                        Xem thêm
                      </button>
                    </div>
                  </div>

                  {/* VARIANTS */}

                  {expanded &&
                    variants.map((variant) => (
                        <div
                          key={variant.id}
                          className={styles.variantRow}
                        >
                          <div
                            className={styles.variantInfo}
                          >
                            <div
                              className={styles.variantIndent}
                            />

                            <div
                              className={styles.variantMeta}
                            >
                              <strong>
                                {getVariantLabel(variant,)}
                              </strong>

                              {variant.skuCode && (
                                <p>
                                  SKU phân loại:{" "}
                                  {
                                    variant.skuCode
                                  }
                                </p>
                              )}
                            </div>
                          </div>

                          <div
                            className={styles.cell}
                          >
                            <span
                              className={styles.price}
                            >
                              ₫ {formatPrice(variant.price,)}
                            </span>
                          </div>

                          <div
                            className={styles.cell}
                          >
                            {
                              variant.stockQuantity
                            }
                          </div>

                          <div />
                        </div>
                      ),
                    )}
                </div>
              );
            },
          )
        )}
      </div>
    </section>
  );
}

export default SellerProductListPage;