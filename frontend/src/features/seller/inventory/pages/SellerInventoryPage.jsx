import {
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";

import fallbackProductImage
  from "../../../../assets/images/product-placeholder.png";

import {
  getSellerProducts,
  getSellerProductVariants,
  updateSellerProductVariant,
} from "../../product/api/sellerProductApi";

import styles from "./SellerInventoryPage.module.css";

function getVariantLabel(variant) {
  const values = [
    variant.variantValue1,
    variant.variantValue2,
  ].filter(Boolean);

  if (values.length === 0) {
    return "Phân loại mặc định";
  }

  return values.join(" / ");
}

function SellerInventoryPage() {
  const [inventoryItems, setInventoryItems] =
    useState([]);

  const [keyword, setKeyword] =
    useState("");

  const [searchKeyword, setSearchKeyword] =
    useState("");

  const [isLoading, setIsLoading] =
    useState(true);

  const [errorMessage, setErrorMessage] =
    useState("");

  const [editingVariantId, setEditingVariantId] =
    useState(null);

  const [stockInput, setStockInput] =
    useState("");

  const [isSaving, setIsSaving] =
    useState(false);

  /*
   * =========================================
   * LOAD INVENTORY
   * =========================================
   */

  const loadInventory = useCallback(async () => {
    try {
      setIsLoading(true);
      setErrorMessage("");

      /*
       * Bước 1:
       * lấy toàn bộ Product của shop.
       */
      const productResponse =
        await getSellerProducts();

      const products =
        Array.isArray(productResponse)
          ? productResponse
          : [];

      /*
       * Bước 2:
       * mỗi Product lấy danh sách Variant.
       */
      const variantResponses =
        await Promise.all(
          products.map(async (product) => {
            const variants =
              await getSellerProductVariants(
                product.id,
              );

            return {
              product,
              variants:
                Array.isArray(variants)
                  ? variants
                  : [],
            };
          }),
        );

      /*
       * Bước 3:
       * chuyển:
       *
       * Product
       *   -> Variant
       *   -> Variant
       *
       * thành một mảng phẳng.
       */
      const items =
        variantResponses.flatMap(
          ({
            product,
            variants,
          }) =>
            variants.map((variant) => ({
              ...variant,

              productId:
                product.id,

              productName:
                product.name,

              productImageUrl:
                product.imageUrl,

              categoryName:
                product.categoryName,
            })),
        );

      setInventoryItems(items);
    } catch (error) {
      console.error(error);

      setErrorMessage(
        error?.response?.data?.message ||
          "Không thể tải dữ liệu tồn kho.",
      );
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadInventory();
  }, [loadInventory]);

  /*
   * =========================================
   * INVENTORY OVERVIEW
   * =========================================
   */

  const inventorySummary =
    useMemo(() => {
      let inStock = 0;
      let lowStock = 0;
      let outOfStock = 0;

      inventoryItems.forEach(
        (item) => {
          const stock =
            Number(
              item.stockQuantity,
            ) || 0;

          if (stock === 0) {
            outOfStock += 1;
          } else if (stock <= 10) {
            lowStock += 1;
          } else {
            inStock += 1;
          }
        },
      );

      return {
        inStock,
        lowStock,
        outOfStock,
      };
    }, [inventoryItems]);

  /*
   * =========================================
   * SEARCH
   * =========================================
   */

  const filteredItems =
    useMemo(() => {
      const value =
        searchKeyword
          .trim()
          .toLowerCase();

      if (!value) {
        return inventoryItems;
      }

      return inventoryItems.filter(
        (item) => {
          const variantText =
            getVariantLabel(item)
              .toLowerCase();

          return (
            item.productName
              ?.toLowerCase()
              .includes(value) ||
            item.skuCode
              ?.toLowerCase()
              .includes(value) ||
            variantText.includes(value)
          );
        },
      );
    }, [
      inventoryItems,
      searchKeyword,
    ]);

  function handleSearch() {
    setSearchKeyword(keyword);
  }

  function handleReset() {
    setKeyword("");
    setSearchKeyword("");
  }

  /*
   * =========================================
   * EDIT STOCK
   * =========================================
   */

  function handleStartEdit(item) {
    setEditingVariantId(item.id);

    setStockInput(
      String(
        item.stockQuantity ?? 0,
      ),
    );
  }

  function handleCancelEdit() {
    setEditingVariantId(null);
    setStockInput("");
  }

  async function handleSaveStock(item) {
    const newStock =
      Number(stockInput);

    if (
      !Number.isInteger(newStock) ||
      newStock < 0
    ) {
      alert(
        "Số lượng tồn kho phải là số nguyên lớn hơn hoặc bằng 0.",
      );

      return;
    }

    try {
      setIsSaving(true);

      /*
       * API hiện tại là PUT,
       * nên phải gửi lại đầy đủ
       * thông tin Variant.
       */
      const updatedVariant =
        await updateSellerProductVariant(
          item.productId,
          item.id,
          {
            variantName1:
              item.variantName1,

            variantValue1:
              item.variantValue1,

            variantName2:
              item.variantName2,

            variantValue2:
              item.variantValue2,

            price:
              Number(item.price),

            stockQuantity:
              newStock,

            skuCode:
              item.skuCode || null,
          },
        );

      /*
       * Không cần load lại toàn trang.
       * Chỉ cập nhật variant vừa sửa.
       */
      setInventoryItems(
        (current) =>
          current.map(
            (inventoryItem) =>
              inventoryItem.id ===
              item.id
                ? {
                    ...inventoryItem,
                    ...updatedVariant,
                  }
                : inventoryItem,
          ),
      );

      setEditingVariantId(null);
      setStockInput("");
    } catch (error) {
      console.error(error);

      alert(
        error?.response?.data?.message ||
          "Không thể cập nhật tồn kho.",
      );
    } finally {
      setIsSaving(false);
    }
  }

  /*
   * =========================================
   * STATE
   * =========================================
   */

  if (isLoading) {
    return (
      <div className={styles.state}>
        Đang tải dữ liệu tồn kho...
      </div>
    );
  }

  if (errorMessage) {
    return (
      <div className={styles.state}>
        <p>{errorMessage}</p>

        <button
          type="button"
          onClick={loadInventory}
        >
          Thử lại
        </button>
      </div>
    );
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