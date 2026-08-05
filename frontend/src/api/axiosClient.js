import axios from "axios";

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


//Tự động gắn JWT vào request.
// Nhờ interceptor này, ta không cần tự thêm bearer token ở từng request.
axiosClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("accessToken");

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
    if (error.response?.status === 401) {
      localStorage.removeItem("accessToken");
      localStorage.removeItem("currentUser");
    }

    return Promise.reject(error);
  },
);

export default axiosClient;