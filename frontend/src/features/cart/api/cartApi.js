import axiosClient from "../../../api/axiosClient";

export async function getCart() {
  const response = await axiosClient.get("/cart");

  return response.data;
}

export async function addCartItem({
  variantId,
  quantity,
}) {
  const response = await axiosClient.post(
    "/cart/items",
    {
      variantId,
      quantity,
    },
  );

  return response.data;
}

export async function updateCartItem(
  cartItemId,
  quantity,
) {
  const response = await axiosClient.put(
    `/cart/items/${cartItemId}`,
    {
      quantity,
    },
  );

  return response.data;
}

export async function deleteCartItem(cartItemId) {
  const response = await axiosClient.delete(
    `/cart/items/${cartItemId}`,
  );

  return response.data;
}

export async function clearCart() {
  const response = await axiosClient.delete(
    "/cart/items",
  );

  return response.data;
}