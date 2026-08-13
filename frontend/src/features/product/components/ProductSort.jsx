import styles from "./ProductSort.module.css";

function ProductSort({
  sortBy,
  currentPage,
  totalPages,
  onSortChange,
  onPageChange,
}) {
  return (
    <div className={styles.sortBar}>
      <div className={styles.left}>
        <span className={styles.label}>
          Sắp xếp theo
        </span>

        <button
          type="button"
          className={
            sortBy === "relevant"
              ? styles.activeButton
              : styles.button
          }
          onClick={() => onSortChange("relevant")}
        >
          Liên Quan
        </button>

        <button
          type="button"
          className={
            sortBy === "newest"
              ? styles.activeButton
              : styles.button
          }
          onClick={() => onSortChange("newest")}
        >
          Mới Nhất
        </button>

        <button
          type="button"
          className={styles.button}
          disabled
          title="Backend chưa có dữ liệu số lượng đã bán"
        >
          Bán Chạy
        </button>

        <select
          className={styles.priceSelect}
          value={
            sortBy === "price_asc" ||
            sortBy === "price_desc"
              ? sortBy
              : ""
          }
          onChange={(event) => {
            if (event.target.value) {
              onSortChange(event.target.value);
            }
          }}
        >
          <option value="">
            Giá
          </option>

          <option value="price_asc">
            Giá: Thấp đến Cao
          </option>

          <option value="price_desc">
            Giá: Cao đến Thấp
          </option>
        </select>
      </div>

      <div className={styles.pageControl}>
        <div className={styles.pageNumber}>
          <span>{currentPage}</span>
          <span>/</span>
          <span>{totalPages}</span>
        </div>

        <button
          type="button"
          disabled={currentPage <= 1}
          onClick={() =>
            onPageChange(currentPage - 1)
          }
          aria-label="Trang trước"
        >
          ‹
        </button>

        <button
          type="button"
          disabled={currentPage >= totalPages}
          onClick={() =>
            onPageChange(currentPage + 1)
          }
          aria-label="Trang sau"
        >
          ›
        </button>
      </div>
    </div>
  );
}

export default ProductSort;