import axiosClient from "../../../api/axiosClient";

export async function startConversation(productId) {
  const response = await axiosClient.post(`/chat/conversations/products/${productId}`);
  return response.data;
}

export async function getCustomerConversations(params = {}) {
  const response = await axiosClient.get("/chat/conversations", { params });
  return response.data;
}

export async function getSellerConversations(params = {}) {
  const response = await axiosClient.get("/seller/chat/conversations", { params });
  return response.data;
}

export async function getConversation(id) {
  const response = await axiosClient.get(`/chat/conversations/${id}`);
  return response.data;
}

export async function getMessages(id, params = {}) {
  const response = await axiosClient.get(`/chat/conversations/${id}/messages`, { params });
  return response.data;
}

export async function sendMessage(id, content) {
  const response = await axiosClient.post(`/chat/conversations/${id}/messages`, { content });
  return response.data;
}

export async function markConversationRead(id) {
  await axiosClient.patch(`/chat/conversations/${id}/read`);
}

export async function getCustomerUnreadCount() {
  const response = await axiosClient.get("/chat/unread-count");
  return response.data;
}

export async function getSellerUnreadCount() {
  const response = await axiosClient.get("/seller/chat/unread-count");
  return response.data;
}
