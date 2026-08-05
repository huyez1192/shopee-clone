import { createBrowserRouter } from "react-router-dom";

import MainLayout from "../layouts/MainLayout";
import HomePage from "../pages/HomePage";
import NotFoundPage from "../pages/NotFoundPage";

import LoginPage from "../features/auth/pages/LoginPage";
import RegisterPage from "../features/auth/pages/RegisterPage";

import ProductListPage from "../features/product/pages/ProductListPage";
import ProductDetailPage from "../features/product/pages/ProductDetailPage";

import CartPage from "../features/cart/pages/CartPage";
import CheckoutPage from "../features/checkout/pages/CheckoutPage";

import OrderListPage from "../features/order/pages/OrderListPage";
import OrderDetailPage from "../features/order/pages/OrderDetailPage";

const router = createBrowserRouter([
  {
    path: "/",
    element: <MainLayout />,
    children: [
      {
        index: true,
        element: <HomePage />,
      },
      {
        path: "login",
        element: <LoginPage />,
      },
      {
        path: "register",
        element: <RegisterPage />,
      },
      {
        path: "products",
        element: <ProductListPage />,
      },
      {
        path: "products/:productId",
        element: <ProductDetailPage />,
      },
      {
        path: "cart",
        element: <CartPage />,
      },
      {
        path: "checkout",
        element: <CheckoutPage />,
      },
      {
        path: "orders",
        element: <OrderListPage />,
      },
      {
        path: "orders/:orderId",
        element: <OrderDetailPage />,
      },
      {
        path: "*",
        element: <NotFoundPage />,
      },
    ],
  },
]);

export default router;