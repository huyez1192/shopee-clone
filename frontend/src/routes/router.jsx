import { createBrowserRouter } from "react-router-dom";

import GuestRoute from "./GuestRoute";
import ProtectedRoute from "./ProtectedRoute";

import MainLayout from "../layouts/MainLayout";

import HomePage from "../pages/HomePage";
import ForbiddenPage from "../pages/ForbiddenPage";
import NotFoundPage from "../pages/NotFoundPage";

import LoginPage from
  "../features/auth/pages/LoginPage";

import RegisterPage from
  "../features/auth/pages/RegisterPage";

import ProductListPage from
  "../features/product/pages/ProductListPage";

import ProductDetailPage from
  "../features/product/pages/ProductDetailPage";

import CartPage from
  "../features/cart/pages/CartPage";

import CheckoutPage from
  "../features/checkout/pages/CheckoutPage";

import OrderListPage from
  "../features/order/pages/OrderListPage";

import OrderDetailPage from
  "../features/order/pages/OrderDetailPage";

import CheckoutLayout from "../layouts/CheckoutLayout";

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
        element: <GuestRoute />,
        children: [
          {
            path: "login",
            element: <LoginPage />,
          },
          {
            path: "register",
            element: <RegisterPage />,
          },
        ],
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
        element: <ProtectedRoute />,
        children: [
          {
            path: "cart",
            element: <CartPage />,
          },
          {
            path: "orders",
            element: <OrderListPage />,
          },
          {
            path: "orders/:orderId",
            element: <OrderDetailPage />,
          },
        ],
      },

      {
        path: "forbidden",
        element: <ForbiddenPage />,
      },

      {
        path: "*",
        element: <NotFoundPage />,
      },
    ],
  },

  {
      element: <ProtectedRoute />,
      children: [
        {
          element: <CheckoutLayout />,

          children: [
            {
              path: "/checkout",
              element: <CheckoutPage />,
            },
          ],
        },
      ],
  }
]);

export default router;