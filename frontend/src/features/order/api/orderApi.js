import axiosClient from "../../../api/axiosClient";

export async function getOrders() {
  const response = await axiosClient.get("/orders");

  return response.data;
}

export async function createOrders({
  addressId,
  cartItemIds,
  paymentMethod,
}) {
  const response = await axiosClient.post(
    "/orders",
    {
      addressId,
      cartItemIds,
      paymentMethod,
    },
  );

  return response.data;
}

export async function getOrderById(orderId) {
  const response = await axiosClient.get(
    `/orders/${orderId}`,
  );

  return response.data;
}