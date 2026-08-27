export const ORDER_TABS = [
  {
    value: "all",
    label: "Tất cả",
  },
  {
    value: "waiting_payment",
    label: "Chờ thanh toán",
  },
  {
    value: "pending",
    label: "Chờ xác nhận",
  },
  {
    value: "confirmed",
    label: "Đang xử lý",
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

export function filterOrdersByTab(
  orders,
  tab,
) {
  switch (tab) {
    case "waiting_payment":
      return orders.filter(
        (order) =>
          order.paymentMethod === "SEPAY" &&
          order.paymentStatus === "UNPAID" &&
          order.status === "PENDING",
      );

    case "pending":
      return orders.filter(
        (order) =>
          order.status === "PENDING" &&
          !(
            order.paymentMethod === "SEPAY" &&
            order.paymentStatus === "UNPAID"
          ),
      );

    case "confirmed":
      return orders.filter(
        (order) =>
          order.status === "CONFIRMED",
      );

    case "shipping":
      return orders.filter(
        (order) =>
          order.status === "SHIPPING",
      );

    case "completed":
      return orders.filter(
        (order) =>
          order.status === "COMPLETED",
      );

    case "cancelled":
      return orders.filter(
        (order) =>
          order.status === "CANCELLED",
      );

    default:
      return orders;
  }
}