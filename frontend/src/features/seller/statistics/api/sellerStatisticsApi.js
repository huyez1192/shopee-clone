import axiosClient from "../../../../api/axiosClient";

export async function getSellerStatisticsOverview(params) {
  const response = await axiosClient.get("/seller/statistics/overview", { params });
  return response.data;
}

export async function getSellerRevenueSeries(params) {
  const response = await axiosClient.get("/seller/statistics/revenue-series", { params });
  return response.data;
}

export async function getSellerTopProducts(params) {
  const response = await axiosClient.get("/seller/statistics/top-products", { params });
  return response.data;
}
