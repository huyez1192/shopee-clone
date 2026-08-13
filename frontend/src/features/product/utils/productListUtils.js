export function filterProducts(
  products,
  {
    keyword = "",
    categoryId = "",
    minPrice = "",
    maxPrice = "",
  },
) {
  const normalizedKeyword = keyword.trim().toLowerCase();

  const minimumPrice =
    minPrice === "" ? null : Number(minPrice);

  const maximumPrice =
    maxPrice === "" ? null : Number(maxPrice);

  return products.filter((product) => {
    const matchesKeyword =
      normalizedKeyword === "" ||
      product.name
        ?.toLowerCase()
        .includes(normalizedKeyword);

    const matchesCategory =
      categoryId === "" ||
      String(product.categoryId) === String(categoryId);

    const productMinimumPrice =
      Number(product.minPrice) || 0;

    const productMaximumPrice =
      Number(product.maxPrice) || productMinimumPrice;

    const matchesMinimumPrice =
      minimumPrice === null ||
      productMaximumPrice >= minimumPrice;

    const matchesMaximumPrice =
      maximumPrice === null ||
      productMinimumPrice <= maximumPrice;

    return (
      matchesKeyword &&
      matchesCategory &&
      matchesMinimumPrice &&
      matchesMaximumPrice
    );
  });
}

export function sortProducts(products, sortBy) {
  const sortedProducts = [...products];

  switch (sortBy) {
    case "price_asc":
      return sortedProducts.sort(
        (firstProduct, secondProduct) =>
          Number(firstProduct.minPrice) -
          Number(secondProduct.minPrice),
      );

    case "price_desc":
      return sortedProducts.sort(
        (firstProduct, secondProduct) =>
          Number(secondProduct.maxPrice) -
          Number(firstProduct.maxPrice),
      );

    case "newest":
      return sortedProducts.sort(
        (firstProduct, secondProduct) =>
          new Date(secondProduct.createdAt).getTime() -
          new Date(firstProduct.createdAt).getTime(),
      );

    case "relevant":
    default:
      return sortedProducts;
  }
}


export function paginateProducts(
  products,
  page,
  pageSize,
) {
  const startIndex = (page - 1) * pageSize;
  const endIndex = startIndex + pageSize;

  return products.slice(startIndex, endIndex);
}

export function calculateTotalPages(
  totalItems,
  pageSize,
) {
  return Math.max(
    1,
    Math.ceil(totalItems / pageSize),
  );
}