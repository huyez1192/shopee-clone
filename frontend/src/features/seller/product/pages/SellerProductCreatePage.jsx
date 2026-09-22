import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";

import { getCategories } from "../../../category/api/categoryApi";
import { queryKeys } from "../../../../query/queryKeys";
import { createSellerProduct, createSellerProductVariant, showSellerProduct } from "../api/sellerProductApi";

import styles from "./SellerProductCreatePage.module.css";

const MAX_VARIANT_GROUPS = 2;
const MAX_VALUES_PER_GROUP = 2;

function createEmptyGroup() {
  return { name: "", values: [""] };
}

function buildVariantRows(groups) {
  if (groups.length === 0) return [];

  const validGroups = groups.map((group) => ({
    name: group.name.trim(),
    values: group.values.map((value) => value.trim()).filter(Boolean),
  }));

  if (validGroups.length === 1) {
    return validGroups[0].values.map((value) => ({
      variantName1: validGroups[0].name,
      variantValue1: value,
      variantName2: null,
      variantValue2: null,
      price: "",
      stockQuantity: "",
    }));
  }

  const [group1, group2] = validGroups;
  const rows = [];

  for (const value1 of group1.values) {
    for (const value2 of group2.values) {
      rows.push({
        variantName1: group1.name,
        variantValue1: value1,
        variantName2: group2.name,
        variantValue2: value2,
        price: "",
        stockQuantity: "",
      });
    }
  }

  return rows;
}

function SellerProductCreatePage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [activeTab, setActiveTab] = useState("basic");
  const [form, setForm] = useState({
    name: "",
    categoryId: "",
    description: "",
    imageUrl: "",
    price: "",
    stockQuantity: "",
  });
  const [variantGroups, setVariantGroups] = useState([]);
  const [variantRows, setVariantRows] = useState([]);
  const [errorMessage, setErrorMessage] = useState("");

  const { data: categories = [] } = useQuery({
    queryKey: queryKeys.categories.all,
    queryFn: getCategories,
    select: (response) => Array.isArray(response) ? response : response?.content || [],
  });

  const createProductMutation = useMutation({
    mutationFn: async ({ productPayload, variantPayloads }) => {
      const product = await createSellerProduct(productPayload);

      for (const variantPayload of variantPayloads) {
        await createSellerProductVariant(product.id, variantPayload);
      }

      await showSellerProduct(product.id);
      return product;
    },
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.seller.products.all }),
        queryClient.invalidateQueries({ queryKey: queryKeys.products.all }),
      ]);

      navigate("/seller/products", { replace: true });
    },
  });

  const isSubmitting = createProductMutation.isPending;
  const hasVariants = variantGroups.length > 0;

  const imagePreview = useMemo(() => {
    const value = form.imageUrl.trim();
    return value || null;
  }, [form.imageUrl]);

  function handleFormChange(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  function handleAddVariantGroup() {
    if (variantGroups.length >= MAX_VARIANT_GROUPS) return;
    setVariantGroups((current) => [...current, createEmptyGroup()]);
  }

  function handleRemoveVariantGroup(groupIndex) {
    setVariantGroups((current) => current.filter((_, index) => index !== groupIndex));
  }

  function handleGroupNameChange(groupIndex, value) {
    setVariantGroups((current) =>
      current.map((group, index) => index === groupIndex ? { ...group, name: value } : group),
    );
  }

  function handleValueChange(groupIndex, valueIndex, value) {
    setVariantGroups((current) =>
      current.map((group, index) => {
        if (index !== groupIndex) return group;

        return {
          ...group,
          values: group.values.map((currentValue, currentIndex) =>
            currentIndex === valueIndex ? value : currentValue,
          ),
        };
      }),
    );
  }

  function handleAddValue(groupIndex) {
    setVariantGroups((current) =>
      current.map((group, index) => {
        if (index !== groupIndex || group.values.length >= MAX_VALUES_PER_GROUP) return group;
        return { ...group, values: [...group.values, ""] };
      }),
    );
  }

  function handleRemoveValue(groupIndex, valueIndex) {
    setVariantGroups((current) =>
      current.map((group, index) => {
        if (index !== groupIndex) return group;
        return { ...group, values: group.values.filter((_, currentIndex) => currentIndex !== valueIndex) };
      }),
    );
  }

  function handleGenerateVariants() {
    for (const group of variantGroups) {
      if (!group.name.trim()) {
        setErrorMessage("Vui lòng nhập tên phân loại.");
        return;
      }

      if (group.values.some((value) => !value.trim())) {
        setErrorMessage("Vui lòng nhập đầy đủ giá trị phân loại.");
        return;
      }
    }

    setErrorMessage("");
    setVariantRows(buildVariantRows(variantGroups));
  }

  function handleVariantChange(rowIndex, field, value) {
    setVariantRows((current) =>
      current.map((row, index) => index === rowIndex ? { ...row, [field]: value } : row),
    );
  }

  function validateForm() {
    if (!form.name.trim()) return "Vui lòng nhập tên sản phẩm.";
    if (!form.categoryId) return "Vui lòng chọn ngành hàng.";

    if (!hasVariants) {
      if (!form.price || Number(form.price) <= 0) return "Giá sản phẩm phải lớn hơn 0.";
      if (form.stockQuantity === "" || Number(form.stockQuantity) < 0) return "Số lượng trong kho không hợp lệ.";
    }

    if (hasVariants) {
      if (variantRows.length === 0) return "Hãy tạo danh sách phân loại trước khi lưu.";

      const invalidVariant = variantRows.some(
        (row) => !row.price || Number(row.price) <= 0 || row.stockQuantity === "" || Number(row.stockQuantity) < 0,
      );

      if (invalidVariant) return "Vui lòng nhập đầy đủ giá và kho cho các phân loại.";
    }

    return "";
  }

  async function handleSubmit(event) {
    event.preventDefault();

    const validationMessage = validateForm();

    if (validationMessage) {
      setErrorMessage(validationMessage);
      return;
    }

    setErrorMessage("");

    const productPayload = {
      name: form.name.trim(),
      description: form.description.trim(),
      categoryId: Number(form.categoryId),
      imageUrl: form.imageUrl.trim() || null,
    };

    const variantPayloads = !hasVariants
      ? [{
          variantName1: null,
          variantValue1: null,
          variantName2: null,
          variantValue2: null,
          price: Number(form.price),
          stockQuantity: Number(form.stockQuantity),
          skuCode: null,
        }]
      : variantRows.map((row) => ({
          variantName1: row.variantName1,
          variantValue1: row.variantValue1,
          variantName2: row.variantName2,
          variantValue2: row.variantValue2,
          price: Number(row.price),
          stockQuantity: Number(row.stockQuantity),
          skuCode: null,
        }));

    try {
      await createProductMutation.mutateAsync({ productPayload, variantPayloads });
    } catch (error) {
      console.error(error);
      setErrorMessage(error?.response?.data?.message || "Không thể tạo sản phẩm.");
    }
  }

  return (
    <section className={styles.page}>
      {/* ================= HEADER ================= */}

      <div className={styles.pageHeader}>
        <div>
          <h1>Thêm 1 sản phẩm mới</h1>
          <p>Điền thông tin sản phẩm để đăng bán trên Shopee</p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className={styles.form}>
        {/* ================= TABS ================= */}

        <div className={styles.tabs}>
          <a
            href="#basic"
            className={activeTab === "basic" ? styles.activeTab : ""}
            onClick={() => setActiveTab("basic")}
          >
            Thông tin cơ bản
          </a>

          <a
            href="#sales"
            className={activeTab === "sales" ? styles.activeTab : ""}
            onClick={() => setActiveTab("sales")}
          >
            Thông tin bán hàng
          </a>
        </div>

        {/* ================= BASIC ================= */}

        <section id="basic" className={styles.card}>
          <h2>Thông tin cơ bản</h2>

          {/* IMAGE */}

          <div className={styles.formRow}>
            <label>
              <span>*</span>
              Ảnh sản phẩm
            </label>

            <div className={styles.imageArea}>
              <div className={styles.imagePreview}>
                {imagePreview ? (
                  <img
                    src={imagePreview}
                    alt="Ảnh sản phẩm"
                    onError={(event) => {
                      event.currentTarget.style.display = "none";
                    }}
                  />
                ) : (
                  <>
                    <div className={styles.imageIcon}>＋</div>
                    <span>Thêm hình ảnh</span>
                  </>
                )}
              </div>

              <div className={styles.imageInput}>
                <input
                  type="url"
                  name="imageUrl"
                  value={form.imageUrl}
                  placeholder="Nhập URL ảnh sản phẩm"
                  onChange={handleFormChange}
                />

                <small>Phiên bản hiện tại sử dụng URL ảnh.</small>
              </div>
            </div>
          </div>

          {/* NAME */}

          <div className={styles.formRow}>
            <label htmlFor="name">
              <span>*</span>
              Tên sản phẩm
            </label>

            <div className={styles.inputWrapper}>
              <input
                id="name"
                name="name"
                value={form.name}
                maxLength={120}
                placeholder="Tên sản phẩm"
                onChange={handleFormChange}
              />

              <span className={styles.counter}>{form.name.length}/120</span>
            </div>
          </div>

          {/* CATEGORY */}

          <div className={styles.formRow}>
            <label htmlFor="categoryId">
              <span>*</span>
              Ngành hàng
            </label>

            <select id="categoryId" name="categoryId" value={form.categoryId} onChange={handleFormChange}>
              <option value="">Chọn ngành hàng</option>

              {categories.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </select>
          </div>

          {/* DESCRIPTION */}

          <div className={styles.formRow}>
            <label htmlFor="description">Mô tả sản phẩm</label>

            <div className={styles.textareaWrapper}>
              <textarea
                id="description"
                name="description"
                value={form.description}
                maxLength={5000}
                rows={8}
                placeholder="Nhập mô tả sản phẩm"
                onChange={handleFormChange}
              />

              <span className={styles.textareaCounter}>{form.description.length}/5000</span>
            </div>
          </div>
        </section>

        {/* ================= SALES ================= */}

        <section id="sales" className={styles.card}>
          <h2>Thông tin bán hàng</h2>

          {/* VARIANTS */}

          <div className={styles.formRow}>
            <label>Phân loại hàng</label>

            <div className={styles.variantSection}>
              {variantGroups.length === 0 && (
                <button type="button" className={styles.addVariantButton} onClick={handleAddVariantGroup}>
                  ＋ Thêm nhóm phân loại
                </button>
              )}

              {variantGroups.map((group, groupIndex) => (
                <div key={groupIndex} className={styles.variantGroup}>
                  <div className={styles.variantGroupHeader}>
                    <strong>Nhóm phân loại {groupIndex + 1}</strong>

                    <button type="button" onClick={() => handleRemoveVariantGroup(groupIndex)}>
                      Xóa
                    </button>
                  </div>

                  <input
                    value={group.name}
                    placeholder={groupIndex === 0 ? "Ví dụ: Màu sắc" : "Ví dụ: Kích thước"}
                    onChange={(event) => handleGroupNameChange(groupIndex, event.target.value)}
                  />

                  <div className={styles.variantValues}>
                    {group.values.map((value, valueIndex) => (
                      <div key={valueIndex} className={styles.variantValue}>
                        <input
                          value={value}
                          placeholder="Nhập giá trị"
                          onChange={(event) => handleValueChange(groupIndex, valueIndex, event.target.value)}
                        />

                        {group.values.length > 1 && (
                          <button type="button" onClick={() => handleRemoveValue(groupIndex, valueIndex)}>
                            ×
                          </button>
                        )}
                      </div>
                    ))}
                  </div>

                  {group.values.length < MAX_VALUES_PER_GROUP && (
                    <button type="button" className={styles.addValueButton} onClick={() => handleAddValue(groupIndex)}>
                      ＋ Thêm giá trị
                    </button>
                  )}
                </div>
              ))}

              {variantGroups.length > 0 && (
                <div className={styles.variantActions}>
                  {variantGroups.length < MAX_VARIANT_GROUPS && (
                    <button type="button" className={styles.addVariantButton} onClick={handleAddVariantGroup}>
                      ＋ Thêm nhóm phân loại
                    </button>
                  )}

                  <button type="button" className={styles.generateButton} onClick={handleGenerateVariants}>
                    Tạo danh sách phân loại
                  </button>
                </div>
              )}
            </div>
          </div>

          {/* NO VARIANTS */}

          {!hasVariants && (
            <>
              <div className={styles.formRow}>
                <label htmlFor="price">
                  <span>*</span>
                  Giá
                </label>

                <div className={styles.moneyInput}>
                  <span>₫</span>

                  <input
                    id="price"
                    type="number"
                    min="1"
                    name="price"
                    value={form.price}
                    placeholder="Nhập giá"
                    onChange={handleFormChange}
                  />
                </div>
              </div>

              <div className={styles.formRow}>
                <label htmlFor="stockQuantity">
                  <span>*</span>
                  Số lượng trong kho
                </label>

                <input
                  id="stockQuantity"
                  type="number"
                  min="0"
                  name="stockQuantity"
                  value={form.stockQuantity}
                  placeholder="Nhập số lượng"
                  onChange={handleFormChange}
                />
              </div>
            </>
          )}

          {/* VARIANT TABLE */}

          {hasVariants && variantRows.length > 0 && (
            <div className={styles.variantTableSection}>
              <h3>Danh sách phân loại</h3>

              <div className={styles.variantTable}>
                <div className={styles.variantTableHeader}>
                  <span>Phân loại</span>
                  <span>Giá</span>
                  <span>Kho hàng</span>
                </div>

                {variantRows.map((row, rowIndex) => (
                  <div
                    key={`${row.variantValue1}-${row.variantValue2}-${rowIndex}`}
                    className={styles.variantTableRow}
                  >
                    <div className={styles.variantLabel}>
                      <strong>{row.variantValue1}</strong>

                      {row.variantValue2 && <span>/ {row.variantValue2}</span>}
                    </div>

                    <div className={styles.tableInput}>
                      <span>₫</span>

                      <input
                        type="number"
                        min="1"
                        value={row.price}
                        placeholder="Giá"
                        onChange={(event) => handleVariantChange(rowIndex, "price", event.target.value)}
                      />
                    </div>

                    <input
                      type="number"
                      min="0"
                      value={row.stockQuantity}
                      placeholder="Kho"
                      onChange={(event) => handleVariantChange(rowIndex, "stockQuantity", event.target.value)}
                    />
                  </div>
                ))}
              </div>
            </div>
          )}
        </section>

        {/* ================= ERROR ================= */}

        {errorMessage && <div className={styles.errorMessage}>{errorMessage}</div>}

        {/* ================= FOOTER ================= */}

        <div className={styles.footer}>
          <button type="button" className={styles.cancelButton} onClick={() => navigate("/seller/products")}>
            Hủy
          </button>

          <button type="submit" className={styles.submitButton} disabled={isSubmitting}>
            {isSubmitting ? "Đang lưu..." : "Lưu & Hiển thị"}
          </button>
        </div>
      </form>
    </section>
  );
}

export default SellerProductCreatePage;