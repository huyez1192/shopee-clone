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

  payment: {
    detail: (paymentCode) => ["payment", paymentCode],
  },

  seller: {
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