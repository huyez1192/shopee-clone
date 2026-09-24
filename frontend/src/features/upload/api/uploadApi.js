import axiosClient from "../../../api/axiosClient";

export async function uploadImage(file, purpose) {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("purpose", purpose);

  const response = await axiosClient.post("/uploads/images", formData);

  return response.data;
}
