import axiosClient from "../../../../api/axiosClient";

export async function getSellerOrders() {
  const response = await axiosClient.get(
    "/seller/orders",
  );

  return response.data;
}

export async function getSellerOrderById(orderId) {
  const response = await axiosClient.get(
    `/seller/orders/${orderId}`,
  );

  return response.data;
}

export async function confirmSellerOrder(orderId) {
  const response = await axiosClient.patch(
    `/seller/orders/${orderId}/confirm`,
  );

  return response.data;
}

export async function shipSellerOrder(orderId) {
  const response = await axiosClient.patch(
    `/seller/orders/${orderId}/ship`,
  );

  return response.data;
}

export async function completeSellerOrder(orderId) {
  const response = await axiosClient.patch(
    `/seller/orders/${orderId}/complete`,
  );

  return response.data;
}