import { createBrowserRouter } from "react-router-dom";

import GuestRoute from "./GuestRoute";
import ProtectedRoute from "./ProtectedRoute";
import RoleRoute from "./RoleRoute";

import MainLayout from "../layouts/MainLayout";
import CheckoutLayout from "../layouts/CheckoutLayout";
import SellerLayout from "../layouts/SellerLayout";
import AuthLayout from "../layouts/AuthLayout";

import HomePage from "../pages/HomePage";
import ForbiddenPage from "../pages/ForbiddenPage";
import NotFoundPage from "../pages/NotFoundPage";

import LoginPage from "../features/auth/pages/LoginPage";
import RegisterPage from "../features/auth/pages/RegisterPage";

import ProductListPage from "../features/product/pages/ProductListPage";
import ProductDetailPage from "../features/product/pages/ProductDetailPage";

import CartPage from "../features/cart/pages/CartPage";

import CheckoutPage from "../features/checkout/pages/CheckoutPage";

import OrderListPage from "../features/order/pages/OrderListPage";
import OrderDetailPage from "../features/order/pages/OrderDetailPage";

import SePayPaymentPage from "../features/payment/pages/SePayPaymentPage";
import PaymentSuccessPage from "../features/payment/pages/PaymentSuccessPage";

import SellerOrderListPage from "../features/seller/order/pages/SellerOrderListPage";
import SellerOrderDetailPage from "../features/seller/order/pages/SellerOrderDetailPage";
import SellerProductCreatePage from "../features/seller/product/pages/SellerProductCreatePage";
import SellerProductListPage from "../features/seller/product/pages/SellerProductListPage";
import SellerInventoryPage from "../features/seller/inventory/pages/SellerInventoryPage";


const router = createBrowserRouter([
  // =====================================================
  // MAIN
  // =====================================================
  {
    path: "/",
    element: <MainLayout />,

    children: [
      {
        index: true,
        element: <HomePage />,
      },



      // =====================================================
      // PRODUCT
      // =====================================================
      {
        path: "products",
        element: <ProductListPage />,
      },

      {
        path: "products/:productId",
        element: <ProductDetailPage />,
      },

      // =====================================================
      // USER ĐÃ ĐĂNG NHẬP
      // =====================================================
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

      // =====================================================
      // ERROR
      // =====================================================
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

  // =====================================================
  // CHECKOUT / PAYMENT
  // =====================================================
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

          {
            path: "/payment/sepay",
            element: <SePayPaymentPage />,
          },

          {
            path: "/payment/success",
            element: <PaymentSuccessPage />,
          },
        ],
      },
    ],
  },

  // =====================================================
  // SELLER
  // =====================================================
  {
    element: (
      <RoleRoute allowedRoles={["SELLER"]} />
    ),

    children: [
        {
          path: "/seller",
          element: <SellerLayout />,

          children: [
            {
              path: "orders",
              element: <SellerOrderListPage />,
            },
            {
              path: "orders/:orderId",
              element: <SellerOrderDetailPage />,
            },
            {
              path: "products",
              element: <SellerProductListPage />,
            },
            {
              path: "products/create",
              element: <SellerProductCreatePage />,
            },
            {
              path: "inventory",
              element: <SellerInventoryPage />,
            },
          ],
        },
      ],
  },

  // =====================================================
  // GUEST
  // =====================================================
  {
      element: <GuestRoute />,

      children: [
          {
            element: <AuthLayout />,

            children: [
              {
                path: "/login",
                element: <LoginPage />,
              },

              {
                path: "/register",
                element: <RegisterPage />,
              },
            ],
          },
      ],
    },
]);

export default router;