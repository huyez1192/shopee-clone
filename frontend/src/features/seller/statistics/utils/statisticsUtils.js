export const ORDER_STATUS_LABELS = {
  PENDING: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  SHIPPING: "Đang giao",
  COMPLETED: "Hoàn thành",
  CANCELLED: "Đã hủy",
};

export const ORDER_STATUSES = Object.keys(ORDER_STATUS_LABELS);

export function formatCurrency(value) {
  const number = Number(value);
  return `${new Intl.NumberFormat("vi-VN").format(Number.isFinite(number) ? number : 0)} ₫`;
}

export function formatNumber(value) {
  const number = Number(value);
  return new Intl.NumberFormat("vi-VN").format(Number.isFinite(number) ? number : 0);
}

export function formatLocalDateParam(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function getDateRangePreset(days) {
  const toDate = new Date();
  const fromDate = new Date(toDate.getFullYear(), toDate.getMonth(), toDate.getDate());
  fromDate.setDate(fromDate.getDate() - (days - 1));
  return { from: formatLocalDateParam(fromDate), to: formatLocalDateParam(toDate) };
}

export function getPercentage(count, total) {
  const safeCount = Number(count) || 0;
  const safeTotal = Number(total) || 0;
  return safeTotal > 0 ? Math.min(100, (safeCount / safeTotal) * 100) : 0;
}
