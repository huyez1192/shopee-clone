import axiosClient from "../../../api/axiosClient";

export async function getCategories() {
  const response = await axiosClient.get("/categories");

  return response.data;
}

export async function getCategoryById(categoryId) {
  const response = await axiosClient.get(
    `/categories/${categoryId}`,
  );

  return response.data;
}