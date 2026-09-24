export const queryKeys = {
  auth: {
    currentUser: ["auth", "currentUser"],
  },

  categories: {
    all: ["categories"],
    detail: (categoryId) => ["categories", categoryId],
  },

  products: {
    all: ["products"],
    list: (params = {}) => ["products", "list", params],
    detail: (productId) => ["products", "detail", productId],
    variants: (productId) => ["products", productId, "variants"],
  },

  cart: {
    all: ["cart"],
  },

  addresses: {
    all: ["addresses"],
  },

  checkout: {
    preview: (payload) => ["checkout", "preview", payload],
  },

  orders: {
    all: ["orders"],
    detail: (orderId) => ["orders", orderId],
  },

  notifications: {
    all: ["notifications"],
    list: (params = {}) => ["notifications", "list", params],
    unreadCount: () => ["notifications", "unread-count"],
  },

  reviews: {
    all: ["reviews"],
    product: (productId, params = {}) => ["reviews", "product", productId, params],
    productRoot: (productId) => ["reviews", "product", productId],
    summary: (productId) => ["reviews", "summary", productId],
  },

  payment: {
    detail: (paymentCode) => ["payment", paymentCode],
  },

  seller: {
    reviews: {
      all: ["seller", "reviews"],
      list: (params = {}) => ["seller", "reviews", params],
    },
    orders: {
      all: ["seller", "orders"],
      detail: (orderId) => ["seller", "orders", orderId],
    },

    products: {
      all: ["seller", "products"],
      detail: (productId) => ["seller", "products", productId],
      variants: (productId) => ["seller", "products", productId, "variants"],
    },
  },
};
