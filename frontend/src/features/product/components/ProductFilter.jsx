import styles from "./ProductFilter.module.css";

function ProductFilter({
  categories,
  values,
  errorMessage,
  onChange,
  onSubmit,
  onClear,
}) {
  return (
    <aside className={styles.filter}>
      <h2 className={styles.title}>
        <span aria-hidden="true">▽</span>
        Bộ lọc tìm kiếm
      </h2>

      <form onSubmit={onSubmit}>
        <section className={styles.section}>
          <h3>Theo danh mục</h3>

          <label className={styles.option}>
            <input
              type="radio"
              name="categoryId"
              value=""
              checked={values.categoryId === ""}
              onChange={onChange}
            />

            <span>Tất cả danh mục</span>
          </label>

          <div className={styles.categoryList}>
            {categories.map((category) => (
              <label
                key={category.id}
                className={styles.option}
              >
                <input
                  type="radio"
                  name="categoryId"
                  value={String(category.id)}
                  checked={
                    String(values.categoryId) ===
                    String(category.id)
                  }
                  onChange={onChange}
                />

                <span>{category.name}</span>
              </label>
            ))}
          </div>
        </section>

        <section className={styles.section}>
          <h3>Khoảng giá</h3>

          <div className={styles.priceInputs}>
            <input
              name="minPrice"
              type="number"
              min="0"
              value={values.minPrice}
              onChange={onChange}
              placeholder="₫ TỪ"
              aria-label="Giá thấp nhất"
            />

            <span>—</span>

            <input
              name="maxPrice"
              type="number"
              min="0"
              value={values.maxPrice}
              onChange={onChange}
              placeholder="₫ ĐẾN"
              aria-label="Giá cao nhất"
            />
          </div>

          {errorMessage && (
            <p
              className={styles.error}
              role="alert"
            >
              {errorMessage}
            </p>
          )}

          <button
            type="submit"
            className={styles.applyButton}
          >
            Áp dụng
          </button>
        </section>

        <button
          type="button"
          className={styles.clearButton}
          onClick={onClear}
        >
          Xóa tất cả
        </button>
      </form>
    </aside>
  );
}

export default ProductFilter;