import styles from "./Pagination.module.css";

function createPageItems(currentPage, totalPages) {
  if (totalPages <= 7) {
    return Array.from(
      { length: totalPages },
      (_, index) => index + 1,
    );
  }

  if (currentPage <= 4) {
    return [1, 2, 3, 4, 5, "...", totalPages];
  }

  if (currentPage >= totalPages - 3) {
    return [
      1,
      "...",
      totalPages - 4,
      totalPages - 3,
      totalPages - 2,
      totalPages - 1,
      totalPages,
    ];
  }

  return [
    1,
    "...",
    currentPage - 1,
    currentPage,
    currentPage + 1,
    "...",
    totalPages,
  ];
}

function Pagination({
  currentPage,
  totalPages,
  onPageChange,
}) {
  if (totalPages <= 1) {
    return null;
  }

  const pageItems = createPageItems(
    currentPage,
    totalPages,
  );

  return (
    <nav
      className={styles.pagination}
      aria-label="Phân trang"
    >
      <button
        type="button"
        className={styles.arrow}
        disabled={currentPage === 1}
        onClick={() =>
          onPageChange(currentPage - 1)
        }
        aria-label="Trang trước"
      >
        ‹
      </button>

      {pageItems.map((item, index) => {
        if (item === "...") {
          return (
            <span
              key={`ellipsis-${index}`}
              className={styles.ellipsis}
            >
              ...
            </span>
          );
        }

        return (
          <button
            key={item}
            type="button"
            className={
              item === currentPage
                ? styles.active
                : styles.pageButton
            }
            onClick={() => onPageChange(item)}
          >
            {item}
          </button>
        );
      })}

      <button
        type="button"
        className={styles.arrow}
        disabled={currentPage === totalPages}
        onClick={() =>
          onPageChange(currentPage + 1)
        }
        aria-label="Trang sau"
      >
        ›
      </button>
    </nav>
  );
}

export default Pagination;