import axiosClient from "../../../api/axiosClient";

export async function getAddresses() {
  const response = await axiosClient.get("/addresses");

  return response.data;
}