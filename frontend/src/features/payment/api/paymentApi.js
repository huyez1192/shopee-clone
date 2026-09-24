import axiosClient from "../../../api/axiosClient";

export async function getPayment(paymentCode) {
  const response = await axiosClient.get(`/payments/${paymentCode}`);
  return response.data;
}

export async function getPaymentStatus(paymentCode) {
  const response = await axiosClient.get(
    `/payments/${paymentCode}/status`,
  );

  return response.data;
}
