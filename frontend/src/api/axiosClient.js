import axios from "axios";

import { AUTH_UNAUTHORIZED_EVENT } from "../features/auth/constants/authEvents";

import {clearAuthStorage, getAccessToken,} from "../features/auth/utils/authStorage";


//Chứa địa chỉ backend.
//Đặt kiểu dữ liệu JSON.
//Đặt thời gian timeout.
const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 10000,
});

axiosClient.interceptors.request.use(
  (config) => {
    const token = getAccessToken();

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => Promise.reject(error),
);

axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const hasToken = Boolean(getAccessToken());
    const isUnauthorized = error.response?.status === 401;

    /*
     * Chỉ xóa phiên đăng nhập khi trước đó đang có token.
     * Như vậy, lỗi 401 do nhập sai mật khẩu sẽ không gây ra hành vi không cần thiết.
     */
    if (isUnauthorized && hasToken) {
          clearAuthStorage();

          window.dispatchEvent(new Event(AUTH_UNAUTHORIZED_EVENT),);
        }

        return Promise.reject(error);
  },
);

export default axiosClient;