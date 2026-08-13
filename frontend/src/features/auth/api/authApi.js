//Ba hàm tương ứng với backend:
//    POST /api/auth/login
//    POST /api/auth/register
//    GET  /api/users/me

import axiosClient from "../../../api/axiosClient";

export async function loginRequest(credentials) {
  const response = await axiosClient.post("/auth/login", credentials);

  return response.data;
}

export async function registerRequest(registerData) {
  const response = await axiosClient.post("/auth/register", registerData);

  return response.data;
}

export async function getCurrentUserRequest() {
  const response = await axiosClient.get("/users/me");

  return response.data;
}