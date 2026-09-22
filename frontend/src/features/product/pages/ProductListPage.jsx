import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";

import EmptyState from "../../../components/common/EmptyState";
import ErrorState from "../../../components/common/ErrorState";
import LoadingState from "../../../components/common/LoadingState";
import Pagination from "../../../components/common/Pagination";

import { getApiErrorMessage } from "../../../utils/getApiErrorMessage";
import { queryKeys } from "../../../query/queryKeys";

import { getCategories } from "../../category/api/categoryApi";
import { getProducts } from "../api/productApi";

import ProductFilter from "../components/ProductFilter";
import ProductSort from "../components/ProductSort";
import ProductGrid from "../components/ProductGrid";

import { calculateTotalPages, filterProducts, paginateProducts, sortProducts } from "../utils/productListUtils";

import styles from "./ProductListPage.module.css";

const PAGE_SIZE = 25;

function getPositivePage(value) {
  const parsedPage = Number(value);

  if (!Number.isInteger(parsedPage) || parsedPage < 1) {
    return 1;
  }

  return parsedPage;
}

function ProductListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [filterError, setFilterError] = useState("");

  const keyword = searchParams.get("keyword") || "";
  const categoryId = searchParams.get("categoryId") || "";
  const minPrice = searchParams.get("minPrice") || "";
  const maxPrice = searchParams.get("maxPrice") || "";
  const sortBy = searchParams.get("sortBy") || "relevant";
  const currentPage = getPositivePage(searchParams.get("page"));

  const filterSource = `${keyword}\u0000${categoryId}\u0000${minPrice}\u0000${maxPrice}`;
  const urlFilterForm = {
    keyword,
    categoryId,
    minPrice,
    maxPrice,
  };
  const [filterDraft, setFilterDraft] = useState(null);
  const filterForm = filterDraft?.source === filterSource
    ? filterDraft.values
    : urlFilterForm;

  const {
    data: products = [],
    isLoading: isProductsLoading,
    error: productError,
    refetch: refetchProducts,
  } = useQuery({
    queryKey: queryKeys.products.all,
    queryFn: getProducts,
    select: (response) => Array.isArray(response) ? response : [],
  });

  const {
    data: categories = [],
    isLoading: isCategoriesLoading,
    error: categoryError,
    refetch: refetchCategories,
  } = useQuery({
    queryKey: queryKeys.categories.all,
    queryFn: getCategories,
    select: (response) => Array.isArray(response) ? response : [],
  });

  const isLoading = isProductsLoading || isCategoriesLoading;
  const queryError = productError || categoryError;

  const errorMessage = queryError
    ? getApiErrorMessage(queryError, "Không thể tải danh sách sản phẩm.")
    : "";

  const filteredProducts = useMemo(() => {
    return filterProducts(products, {
      keyword,
      categoryId,
      minPrice,
      maxPrice,
    });
  }, [products, keyword, categoryId, minPrice, maxPrice]);

  const sortedProducts = useMemo(() => {
    return sortProducts(filteredProducts, sortBy);
  }, [filteredProducts, sortBy]);

  const totalPages = calculateTotalPages(sortedProducts.length, PAGE_SIZE);
  const validCurrentPage = Math.min(currentPage, totalPages);

  const visibleProducts = useMemo(() => {
    return paginateProducts(sortedProducts, validCurrentPage, PAGE_SIZE);
  }, [sortedProducts, validCurrentPage]);

  function loadPageData() {
    refetchProducts();
    refetchCategories();
  }

  function handleFilterChange(event) {
    const { name, value } = event.target;

    setFilterDraft({
      source: filterSource,
      values: {
        ...filterForm,
        [name]: value,
      },
    });

    setFilterError("");
  }

  function handleFilterSubmit(event) {
    event.preventDefault();

    const minimumPrice = filterForm.minPrice === "" ? null : Number(filterForm.minPrice);
    const maximumPrice = filterForm.maxPrice === "" ? null : Number(filterForm.maxPrice);

    if (minimumPrice !== null && maximumPrice !== null && minimumPrice > maximumPrice) {
      setFilterError("Giá thấp nhất không được lớn hơn giá cao nhất.");
      return;
    }

    setFilterError("");

    const nextSearchParams = new URLSearchParams();

    if (filterForm.keyword.trim()) {
      nextSearchParams.set("keyword", filterForm.keyword.trim());
    }

    if (filterForm.categoryId) {
      nextSearchParams.set("categoryId", filterForm.categoryId);
    }

    if (filterForm.minPrice) {
      nextSearchParams.set("minPrice", filterForm.minPrice);
    }

    if (filterForm.maxPrice) {
      nextSearchParams.set("maxPrice", filterForm.maxPrice);
    }

    if (sortBy !== "relevant") {
      nextSearchParams.set("sortBy", sortBy);
    }

    nextSearchParams.set("page", "1");
    setFilterDraft(null);
    setSearchParams(nextSearchParams);
  }

  function handleClearFilters() {
    setFilterDraft(null);
    setSearchParams({});
    setFilterError("");
  }

  function handleSortChange(nextSortBy) {
    const nextSearchParams = new URLSearchParams(searchParams);

    if (nextSortBy === "relevant") {
      nextSearchParams.delete("sortBy");
    } else {
      nextSearchParams.set("sortBy", nextSortBy);
    }

    nextSearchParams.set("page", "1");
    setSearchParams(nextSearchParams);
  }

  function handlePageChange(pageNumber) {
    const nextSearchParams = new URLSearchParams(searchParams);

    nextSearchParams.set("page", String(pageNumber));
    setSearchParams(nextSearchParams);

    window.scrollTo({
      top: 0,
      behavior: "smooth",
    });
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
        onRetry={loadPageData}
      />
    );
  }

  return (
    <main className={styles.page}>
      <div className={styles.container}>
        <ProductFilter
          categories={categories}
          values={filterForm}
          errorMessage={filterError}
          onChange={handleFilterChange}
          onSubmit={handleFilterSubmit}
          onClear={handleClearFilters}
        />

        <section className={styles.productContent}>
          <ProductSort
            sortBy={sortBy}
            currentPage={validCurrentPage}
            totalPages={totalPages}
            onSortChange={handleSortChange}
            onPageChange={handlePageChange}
          />

          <div className={styles.productArea}>
            {visibleProducts.length === 0 ? (
              <EmptyState
                title="Không tìm thấy sản phẩm"
                description="Hãy thử thay đổi từ khóa hoặc bộ lọc."
              />
            ) : (
              <ProductGrid products={visibleProducts} />
            )}
          </div>

          <Pagination
            currentPage={validCurrentPage}
            totalPages={totalPages}
            onPageChange={handlePageChange}
          />
        </section>
      </div>
    </main>
  );
}

export default ProductListPage;
