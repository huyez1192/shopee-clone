import axiosClient from "../../../api/axiosClient";

export async function createOrders({
  addressId,
  cartItemIds,
  paymentMethod = "COD",
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