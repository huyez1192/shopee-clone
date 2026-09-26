import { useEffect, useMemo, useRef, useState } from "react";
import { useQueries, useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import LoadingState from "../../../../components/common/LoadingState";
import ErrorState from "../../../../components/common/ErrorState";
import { getApiErrorMessage } from "../../../../utils/getApiErrorMessage";

import fallbackProductImage from "../../../../assets/images/product-placeholder.png";
import { queryKeys } from "../../../../query/queryKeys";

import {
  getSellerProducts,
  getSellerProductVariants,
} from "../api/sellerProductApi";

import styles from "./SellerProductListPage.module.css";

function formatPrice(value) {
  return new Intl.NumberFormat("vi-VN").format(Number(value) || 0);
}

function getVariantLabel(variant) {
  const values = [variant.variantValue1, variant.variantValue2].filter(Boolean);

  if (values.length === 0) {
    return "Mặc định";
  }

  return values.join(" / ");
}

function SellerProductListPage() {
  const [keyword, setKeyword] = useState("");
  const [searchKeyword, setSearchKeyword] = useState("");
  const [expandedProductIds, setExpandedProductIds] = useState([]);

  const reportedVariantErrorsRef = useRef(new Set());

  const {
    data: products = [],
    isLoading,
    error,
    refetch,
  } = useQuery({
    queryKey: queryKeys.seller.products.all,
    queryFn: getSellerProducts,
    select: (response) => Array.isArray(response) ? response : [],
  });

  const variantQueries = useQueries({
    queries: products.map((product) => ({
      queryKey: queryKeys.seller.products.variants(product.id),
      queryFn: () => getSellerProductVariants(product.id),
      enabled: expandedProductIds.includes(product.id),
      select: (response) => Array.isArray(response) ? response : [],
    })),
  });

  const variantsByProduct = useMemo(() => {
    const result = {};

    products.forEach((product, index) => {
      result[product.id] = variantQueries[index]?.data || [];
    });

    return result;
  }, [products, variantQueries]);

  const loadingVariantIds = useMemo(() => {
    return products
      .filter((product, index) => variantQueries[index]?.isFetching)
      .map((product) => product.id);
  }, [products, variantQueries]);

  useEffect(() => {
    variantQueries.forEach((query, index) => {
      if (!query.error) {
        return;
      }

      const productId = products[index]?.id;
      const errorKey = `${productId}-${query.errorUpdatedAt}`;

      if (reportedVariantErrorsRef.current.has(errorKey)) {
        return;
      }

      reportedVariantErrorsRef.current.add(errorKey);

      console.error(query.error);

      alert(
        query.error?.response?.data?.message ||
          "Không thể tải phân loại sản phẩm.",
      );
    });
  }, [products, variantQueries]);

  const filteredProducts = useMemo(() => {
    const value = searchKeyword.trim().toLowerCase();

    if (!value) {
      return products;
    }

    return products.filter(
      (product) =>
        product.name?.toLowerCase().includes(value) ||
        product.categoryName?.toLowerCase().includes(value),
    );
  }, [products, searchKeyword]);

  function loadProducts() {
    refetch();
  }

  function handleSearch() {
    setSearchKeyword(keyword);
  }

  function handleReset() {
    setKeyword("");
    setSearchKeyword("");
  }

  function handleToggleVariants(product) {
    setExpandedProductIds((current) => {
      if (current.includes(product.id)) {
        return current.filter((id) => id !== product.id);
      }

      return [...current, product.id];
    });
  }

  if (isLoading) {
    return <LoadingState message="Đang tải sản phẩm..." />;
  }

  if (error) {
    return <ErrorState message={getApiErrorMessage(error, "Không thể tải danh sách sản phẩm.")} onRetry={loadProducts} />;
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
              setKeyword(event.target.value)
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
          {filteredProducts.length} Sản phẩm
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
              const expanded = expandedProductIds.includes(product.id);

              const loadingVariants = loadingVariantIds.includes(product.id);

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
                        src={product.imageUrl || fallbackProductImage}
                        alt={product.name}
                        className={styles.productImage}
                        onError={(event) => {
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
                          ₫ {formatPrice(product.minPrice)}
                        </span>
                      ) : (
                        <div
                          className={styles.priceRange}
                        >
                          <span>
                            ₫ {formatPrice(product.minPrice)}
                          </span>

                          <span>
                            -
                          </span>

                          <span>
                            ₫ {formatPrice(product.maxPrice)}
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
                                {getVariantLabel(variant)}
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
                              ₫ {formatPrice(variant.price)}
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
