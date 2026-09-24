import axiosClient from "../../../api/axiosClient";

export async function getNotifications(params = {}) {
  const response = await axiosClient.get("/notifications", { params });
  return response.data;
}

export async function getUnreadNotificationCount() {
  const response = await axiosClient.get("/notifications/unread-count");
  return response.data;
}

export async function markNotificationRead(id) {
  const response = await axiosClient.patch(`/notifications/${id}/read`);
  return response.data;
}

export async function markAllNotificationsRead() {
  const response = await axiosClient.patch("/notifications/read-all");
  return response.data;
}
