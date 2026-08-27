export function getOrderStatusLabel(order) {
  if (
    order.status === "CANCELLED"
  ) {
    return "ĐƠN HÀNG ĐÃ HỦY";
  }

  if (
    order.paymentMethod === "SEPAY" &&
    order.paymentStatus === "UNPAID" &&
    order.status === "PENDING"
  ) {
    return "CHỜ THANH TOÁN";
  }

  switch (order.status) {
    case "PENDING":
      return "CHỜ XÁC NHẬN";

    case "CONFIRMED":
      return "NGƯỜI BÁN ĐÃ XÁC NHẬN ĐƠN HÀNG";

    case "SHIPPING":
      return "ĐƠN HÀNG ĐANG ĐƯỢC GIAO";

    case "COMPLETED":
      return "ĐƠN HÀNG ĐÃ HOÀN THÀNH";

    default:
      return order.status;
  }
}

export function getCompletedStep(order) {
  if (order.status === "CANCELLED") {
    return 0;
  }

  if (
    order.paymentMethod === "SEPAY" &&
    order.paymentStatus === "UNPAID"
  ) {
    return 1;
  }

  switch (order.status) {
    case "PENDING":
      return order.paymentStatus === "PAID"
        ? 2
        : 1;

    case "CONFIRMED":
      return 3;

    case "SHIPPING":
      return 4;

    case "COMPLETED":
      return 5;

    default:
      return 1;
  }
}