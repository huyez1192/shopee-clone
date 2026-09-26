import { useMemo, useState } from "react";
import { useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";
import LoadingState from "../../../../components/common/LoadingState";
import ErrorState from "../../../../components/common/ErrorState";
import { getApiErrorMessage } from "../../../../utils/getApiErrorMessage";

import fallbackProductImage from "../../../../assets/images/product-placeholder.png";
import { queryKeys } from "../../../../query/queryKeys";

import {
  getSellerProducts,
  getSellerProductVariants,
  updateSellerProductVariant,
} from "../../product/api/sellerProductApi";

import styles from "./SellerInventoryPage.module.css";

function getVariantLabel(variant) {
  const values = [variant.variantValue1, variant.variantValue2].filter(Boolean);

  if (values.length === 0) {
    return "Phân loại mặc định";
  }

  return values.join(" / ");
}

function SellerInventoryPage() {
  const queryClient = useQueryClient();

  const [keyword, setKeyword] = useState("");
  const [searchKeyword, setSearchKeyword] = useState("");
  const [editingVariantId, setEditingVariantId] = useState(null);
  const [stockInput, setStockInput] = useState("");

  const {
    data: products = [],
    isLoading: isProductsLoading,
    error: productsError,
  } = useQuery({
    queryKey: queryKeys.seller.products.all,
    queryFn: getSellerProducts,
    select: (response) => Array.isArray(response) ? response : response?.content || [],
  });

  const variantQueries = useQueries({
    queries: products.map((product) => ({
      queryKey: queryKeys.seller.products.variants(product.id),
      queryFn: () => getSellerProductVariants(product.id),
      select: (response) => Array.isArray(response) ? response : [],
    })),
  });

  const updateStockMutation = useMutation({
    mutationFn: ({ item, newStock }) =>
      updateSellerProductVariant(item.productId, item.id, {
        variantName1: item.variantName1,
        variantValue1: item.variantValue1,
        variantName2: item.variantName2,
        variantValue2: item.variantValue2,
        price: Number(item.price),
        stockQuantity: newStock,
        skuCode: item.skuCode || null,
      }),

    onSuccess: (updatedVariant, variables) => {
      const { item } = variables;

      queryClient.setQueryData(
        queryKeys.seller.products.variants(item.productId),
        (currentVariants) => {
          if (!Array.isArray(currentVariants)) {
            return currentVariants;
          }

          return currentVariants.map((variant) =>
            variant.id === item.id ? { ...variant, ...updatedVariant } : variant,
          );
        },
      );

      queryClient.invalidateQueries({
        queryKey: queryKeys.seller.products.all,
        exact: true,
      });

      queryClient.invalidateQueries({
        queryKey: queryKeys.products.all,
      });
    },
  });

  const inventoryItems = useMemo(() => {
    return products.flatMap((product, index) => {
      const variants = variantQueries[index]?.data || [];

      return variants.map((variant) => ({
        ...variant,
        productId: product.id,
        productName: product.name,
        productImageUrl: product.imageUrl,
        categoryName: product.categoryName,
      }));
    });
  }, [products, variantQueries]);

  const variantsLoading = variantQueries.some((query) => query.isLoading);
  const variantsError = variantQueries.find((query) => query.error)?.error;

  const isLoading = isProductsLoading || variantsLoading;
  const queryError = productsError || variantsError;

  const isSaving = updateStockMutation.isPending;

  const inventorySummary = useMemo(() => {
    let inStock = 0;
    let lowStock = 0;
    let outOfStock = 0;

    inventoryItems.forEach((item) => {
      const stock = Number(item.stockQuantity) || 0;

      if (stock === 0) {
        outOfStock += 1;
      } else if (stock <= 10) {
        lowStock += 1;
      } else {
        inStock += 1;
      }
    });

    return { inStock, lowStock, outOfStock };
  }, [inventoryItems]);

  const filteredItems = useMemo(() => {
    const value = searchKeyword.trim().toLowerCase();

    if (!value) {
      return inventoryItems;
    }

    return inventoryItems.filter((item) => {
      const variantText = getVariantLabel(item).toLowerCase();

      return (
        item.productName?.toLowerCase().includes(value) ||
        item.skuCode?.toLowerCase().includes(value) ||
        variantText.includes(value)
      );
    });
  }, [inventoryItems, searchKeyword]);

  function loadInventory() {
    queryClient.invalidateQueries({
      queryKey: queryKeys.seller.products.all,
    });
  }

  function handleSearch() {
    setSearchKeyword(keyword);
  }

  function handleReset() {
    setKeyword("");
    setSearchKeyword("");
  }

  function handleStartEdit(item) {
    setEditingVariantId(item.id);
    setStockInput(String(item.stockQuantity ?? 0));
  }

  function handleCancelEdit() {
    setEditingVariantId(null);
    setStockInput("");
  }

  async function handleSaveStock(item) {
    const newStock = Number(stockInput);

    if (!Number.isInteger(newStock) || newStock < 0) {
      alert("Số lượng tồn kho phải là số nguyên lớn hơn hoặc bằng 0.");
      return;
    }

    try {
      await updateStockMutation.mutateAsync({ item, newStock });
      setEditingVariantId(null);
      setStockInput("");
    } catch (error) {
      console.error(error);
      alert(error?.response?.data?.message || "Không thể cập nhật tồn kho.");
    }
  }

  if (isLoading) {
    return <LoadingState message="Đang tải dữ liệu tồn kho..." />;
  }

  if (queryError) {
    return <ErrorState message={getApiErrorMessage(queryError, "Không thể tải dữ liệu tồn kho.")} onRetry={loadInventory} />;
  }

  return (
    <section className={styles.page}>
      {/* ===================================
          OVERVIEW
      =================================== */}

      <section className={styles.overview}>
        <div className={styles.overviewHeader}>
          <h1>Tổng quan tồn kho</h1>

          <span>
            Tổng{" "}
            {inventoryItems.length}{" "}
            phân loại
          </span>
        </div>

        <div className={styles.summaryGrid}>
          <div className={styles.summaryCard}>
            <strong>
              {
                inventorySummary.inStock
              }
            </strong>

            <span>
              Phân loại còn hàng
            </span>

            <small>
              Tồn kho trên 10 sản phẩm
            </small>
          </div>

          <div
            className={`${styles.summaryCard} ${styles.lowStockCard}`}
          >
            <strong>
              {
                inventorySummary.lowStock
              }
            </strong>

            <span>
              Phân loại sắp hết hàng
            </span>

            <small>
              Còn từ 1 đến 10 sản phẩm
            </small>
          </div>

          <div
            className={`${styles.summaryCard} ${styles.outOfStockCard}`}
          >
            <strong>
              {
                inventorySummary.outOfStock
              }
            </strong>

            <span>
              Phân loại hết hàng
            </span>

            <small>
              Tồn kho bằng 0
            </small>
          </div>
        </div>
      </section>

      {/* ===================================
          INVENTORY MANAGEMENT
      =================================== */}

      <section
        className={
          styles.inventorySection
        }
      >
        <div
          className={
            styles.sectionHeader
          }
        >
          <div>
            <h2>
              Quản Lý Tồn Kho
            </h2>

            <p>
              Theo dõi và cập nhật số
              lượng tồn kho của từng
              phân loại sản phẩm.
            </p>
          </div>

          <button
            type="button"
            className={
              styles.refreshButton
            }
            onClick={loadInventory}
          >
            ↻ Làm mới
          </button>
        </div>

        {/* SEARCH */}

        <div className={styles.toolbar}>
          <input
            type="text"
            value={keyword}
            placeholder="Tìm tên sản phẩm, phân loại hoặc SKU..."
            onChange={(event) =>
              setKeyword(
                event.target.value,
              )
            }
            onKeyDown={(event) => {
              if (
                event.key === "Enter"
              ) {
                handleSearch();
              }
            }}
          />

          <button
            type="button"
            className={
              styles.searchButton
            }
            onClick={handleSearch}
          >
            Tìm kiếm
          </button>

          <button
            type="button"
            className={
              styles.resetButton
            }
            onClick={handleReset}
          >
            Đặt lại
          </button>
        </div>

        <div className={styles.resultCount}>
          {filteredItems.length} phân loại
        </div>

        {/* ===================================
            TABLE
        =================================== */}

        <div className={styles.table}>
          <div
            className={
              styles.tableHeader
            }
          >
            <div>
              Tên sản phẩm
            </div>

            <div>
              Tồn kho có sẵn
            </div>

            <div>
              Thao tác
            </div>
          </div>

          {filteredItems.length === 0 ? (
            <div
              className={
                styles.empty
              }
            >
              Không có sản phẩm phù hợp.
            </div>
          ) : (
            filteredItems.map(
              (item) => {
                const editing =
                  editingVariantId ===
                  item.id;

                const stock =
                  Number(
                    item.stockQuantity,
                  ) || 0;

                return (
                  <div
                    key={item.id}
                    className={
                      styles.tableRow
                    }
                  >
                    {/* PRODUCT */}

                    <div
                      className={
                        styles.product
                      }
                    >
                      <img
                        src={
                          item.productImageUrl ||
                          fallbackProductImage
                        }
                        alt={
                          item.productName
                        }
                        onError={(
                          event,
                        ) => {
                          event.currentTarget.src =
                            fallbackProductImage;
                        }}
                      />

                      <div
                        className={
                          styles.productInfo
                        }
                      >
                        <strong>
                          {
                            item.productName
                          }
                        </strong>

                        <span
                          className={
                            styles.variant
                          }
                        >
                          {getVariantLabel(
                            item,
                          )}
                        </span>

                        {item.skuCode && (
                          <span
                            className={
                              styles.sku
                            }
                          >
                            SKU:{" "}
                            {
                              item.skuCode
                            }
                          </span>
                        )}
                      </div>
                    </div>

                    {/* STOCK */}

                    <div
                      className={
                        styles.stockCell
                      }
                    >
                      {editing ? (
                        <input
                          type="number"
                          min="0"
                          value={
                            stockInput
                          }
                          onChange={(
                            event,
                          ) =>
                            setStockInput(
                              event
                                .target
                                .value,
                            )
                          }
                        />
                      ) : (
                        <>
                          <strong>
                            {stock}
                          </strong>

                          {stock === 0 && (
                            <span
                              className={
                                styles.outOfStock
                              }
                            >
                              Hết hàng
                            </span>
                          )}

                          {stock > 0 &&
                            stock <= 10 && (
                              <span
                                className={
                                  styles.lowStock
                                }
                              >
                                Sắp hết hàng
                              </span>
                            )}
                        </>
                      )}
                    </div>

                    {/* ACTION */}

                    <div
                      className={
                        styles.actions
                      }
                    >
                      {editing ? (
                        <>
                          <button
                            type="button"
                            className={
                              styles.saveButton
                            }
                            disabled={
                              isSaving
                            }
                            onClick={() =>
                              handleSaveStock(
                                item,
                              )
                            }
                          >
                            {isSaving
                              ? "Đang lưu..."
                              : "Lưu"}
                          </button>

                          <button
                            type="button"
                            className={
                              styles.cancelButton
                            }
                            disabled={
                              isSaving
                            }
                            onClick={
                              handleCancelEdit
                            }
                          >
                            Hủy
                          </button>
                        </>
                      ) : (
                        <button
                          type="button"
                          className={
                            styles.editButton
                          }
                          onClick={() =>
                            handleStartEdit(
                              item,
                            )
                          }
                        >
                          Sửa
                        </button>
                      )}
                    </div>
                  </div>
                );
              },
            )
          )}
        </div>
      </section>
    </section>
  );
}

export default SellerInventoryPage;
