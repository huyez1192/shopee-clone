import { formatNotificationTime, getNotificationIcon } from "../utils/notificationUtils";

import styles from "./NotificationItem.module.css";

function NotificationIcon({ type }) {
  const icon = getNotificationIcon(type);

  return (
    <span className={`${styles.icon} ${styles[icon]}`} aria-hidden="true">
      {icon === "star" ? "★" : icon === "shop" ? "S" : "□"}
    </span>
  );
}

function NotificationItem({ notification, onSelect, compact = false, disabled = false }) {
  return (
    <button
      type="button"
      className={`${styles.item} ${!notification.isRead ? styles.unread : ""} ${compact ? styles.compact : ""}`}
      onClick={() => onSelect(notification)}
      disabled={disabled}
    >
      <NotificationIcon type={notification.type} />

      <span className={styles.content}>
        <span className={styles.titleRow}>
          <strong>{notification.title}</strong>
          {!notification.isRead && <span className={styles.unreadDot} aria-label="Chưa đọc" />}
        </span>
        <span className={styles.message}>{notification.message}</span>
        <time dateTime={notification.createdAt}>{formatNotificationTime(notification.createdAt)}</time>
      </span>

      {!compact && <span className={styles.details}>Xem Chi Tiết</span>}
    </button>
  );
}

export default NotificationItem;
