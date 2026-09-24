export function isSafeInternalTarget(targetUrl) {
  if (typeof targetUrl !== "string") return false;

  const target = targetUrl.trim();
  return target.startsWith("/") && !target.startsWith("//") && !target.includes("\\");
}

export function formatNotificationTime(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";

  const elapsed = Date.now() - date.getTime();
  const minute = 60 * 1000;
  const hour = 60 * minute;
  const day = 24 * hour;

  if (elapsed >= 0 && elapsed < minute) return "Vừa xong";
  if (elapsed >= minute && elapsed < hour) return `${Math.floor(elapsed / minute)} phút trước`;
  if (elapsed >= hour && elapsed < day) return `${Math.floor(elapsed / hour)} giờ trước`;
  if (elapsed >= day && elapsed < 2 * day) return "Hôm qua";

  return new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

export function getNotificationIcon(type) {
  if (type === "PRODUCT_REVIEWED") return "star";
  if (type === "SHOP_APPROVED" || type === "SHOP_REJECTED") return "shop";
  return "order";
}
