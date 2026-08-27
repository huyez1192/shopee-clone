export const SELLER_ORDER_TABS = [
  {
    value: "all",
    label: "Tất cả",
  },
  {
    value: "pending",
    label: "Chờ xác nhận",
  },
  {
    value: "confirmed",
    label: "Chờ lấy hàng",
  },
  {
    value: "shipping",
    label: "Đang giao",
  },
  {
    value: "completed",
    label: "Hoàn thành",
  },
  {
    value: "cancelled",
    label: "Đã hủy",
  },
];

export function filterSellerOrders(
  orders,
  tab,
) {
  if (tab === "all") {
    return orders;
  }

  return orders.filter(
    (order) =>
      order.status === tab.toUpperCase(),
  );
}

export function getSellerOrderStatusLabel(order) {
  if (
    order.paymentMethod === "SEPAY" &&
    order.paymentStatus === "UNPAID" &&
    order.status === "PENDING"
  ) {
    return "CHỜ KHÁCH THANH TOÁN";
  }

  switch (order.status) {
    case "PENDING":
      return "CHỜ XÁC NHẬN";

    case "CONFIRMED":
      return "CHỜ LẤY HÀNG";

    case "SHIPPING":
      return "ĐANG GIAO";

    case "COMPLETED":
      return "HOÀN THÀNH";

    case "CANCELLED":
      return "ĐÃ HỦY";

    default:
      return order.status;
  }
}