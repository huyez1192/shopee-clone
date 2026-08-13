import axiosClient from "../../../api/axiosClient";

export async function getCheckoutPreview({
  addressId,
  cartItemIds,
}) {
  const response = await axiosClient.post(
    "/checkout/preview",
    {
      addressId,
      cartItemIds,
    },
  );

  return response.data;
}