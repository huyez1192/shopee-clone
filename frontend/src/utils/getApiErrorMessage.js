export function getApiErrorMessage(
  error,
  fallbackMessage = "Đã xảy ra lỗi.",
) {
  return (
    error.response?.data?.message ||
    error.message ||
    fallbackMessage
  );
}

export function getApiFieldErrors(error) {
  return error.response?.data?.errors ?? {};
}