import axiosClient from "../../../api/axiosClient";

export async function getProducts(params = {}) {
  const response = await axiosClient.get("/products", {
    params,
  });

  return response.data;
}

export async function getProductById(productId) {
  const response = await axiosClient.get(
    `/products/${productId}`,
  );

  return response.data;
}

export async function getProductVariants(productId) {
  const response = await axiosClient.get(
    `/products/${productId}/variants`,
  );

  return response.data;
}