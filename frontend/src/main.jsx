import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { RouterProvider } from "react-router-dom";

import { QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";

import AuthProvider from "./features/auth/context/AuthProvider";
import RealtimeProvider from "./features/realtime/RealtimeProvider";
import router from "./routes/router";
import "./index.css";

import queryClient from "./query/queryClient";

createRoot(document.getElementById("root")).render(
  <StrictMode>
      <QueryClientProvider client={queryClient}>
          <AuthProvider>
              <RealtimeProvider>
                  <RouterProvider router={router} />
              </RealtimeProvider>
          </AuthProvider>

          <ReactQueryDevtools initialIsOpen={false} />
      </QueryClientProvider>
  </StrictMode>,
);
