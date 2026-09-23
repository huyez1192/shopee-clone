import axiosClient from "../../../api/axiosClient";

export async function getProductReviews(productId, params = {}) {
  const response = await axiosClient.get(`/products/${productId}/reviews`, { params });
  return response.data;
}

export async function getProductReviewSummary(productId) {
  const response = await axiosClient.get(`/products/${productId}/reviews/summary`);
  return response.data;
}

export async function createReview(payload) {
  const response = await axiosClient.post("/reviews", payload);
  return response.data;
}

export async function getSellerReviews(params = {}) {
  const response = await axiosClient.get("/seller/reviews", { params });
  return response.data;
}

export async function replyToReview({ reviewId, reply }) {
  const response = await axiosClient.patch(`/seller/reviews/${reviewId}/reply`, { reply });
  return response.data;
}
