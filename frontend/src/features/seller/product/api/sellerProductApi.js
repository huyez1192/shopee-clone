import axiosClient from "../../../../api/axiosClient";

export async function getSellerProducts() {
  const response = await axiosClient.get(
    "/seller/products",
  );

  return response.data;
}

export async function getSellerProductVariants(
  productId,
) {
  const response = await axiosClient.get(
    `/seller/products/${productId}/variants`,
  );

  return response.data;
}

export async function createSellerProduct(payload) {
  const response = await axiosClient.post(
    "/seller/products",
    payload,
  );

  return response.data;
}

export async function createSellerProductVariant(
  productId,
  payload,
) {
  const response = await axiosClient.post(
    `/seller/products/${productId}/variants`,
    payload,
  );

  return response.data;
}

export async function showSellerProduct(productId) {
  const response = await axiosClient.patch(
    `/seller/products/${productId}/show`,
  );

  return response.data;
}

export async function updateSellerProductVariant(
  productId,
  variantId,
  payload,
) {
  const response = await axiosClient.put(
    `/seller/products/${productId}/variants/${variantId}`,
    payload,
  );

  return response.data;
}