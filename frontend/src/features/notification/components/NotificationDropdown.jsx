import { useEffect, useRef } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";

import { queryKeys } from "../../../query/queryKeys";
import { getApiErrorMessage } from "../../../utils/getApiErrorMessage";
import { getNotifications, markNotificationRead } from "../api/notificationApi";
import { isSafeInternalTarget } from "../utils/notificationUtils";
import NotificationItem from "./NotificationItem";

import styles from "./NotificationDropdown.module.css";

const DROPDOWN_PARAMS = { page: 0, size: 5 };

function NotificationDropdown({ onClose }) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const panelRef = useRef(null);
  const query = useQuery({
    queryKey: queryKeys.notifications.list(DROPDOWN_PARAMS),
    queryFn: () => getNotifications(DROPDOWN_PARAMS),
  });
  const mutation = useMutation({
    mutationFn: markNotificationRead,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.notifications.all });
    },
  });

  useEffect(() => {
    function handleKeyDown(event) {
      if (event.key === "Escape") onClose();
    }
    function handlePointerDown(event) {
      if (!panelRef.current?.parentElement?.contains(event.target)) onClose();
    }
    document.addEventListener("keydown", handleKeyDown);
    document.addEventListener("mousedown", handlePointerDown);
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
      document.removeEventListener("mousedown", handlePointerDown);
    };
  }, [onClose]);

  async function selectNotification(notification) {
    if (!notification.isRead) {
      try {
        await mutation.mutateAsync(notification.id);
      } catch {
        return;
      }
    }
    onClose();
    if (isSafeInternalTarget(notification.targetUrl)) navigate(notification.targetUrl.trim());
  }

  const notifications = query.data?.content || [];

  return (
    <section ref={panelRef} className={styles.dropdown} aria-label="Thông báo">
      <header>Thông Báo Mới Nhận</header>
      <div className={styles.body}>
        {query.isLoading && <p className={styles.state}>Đang tải thông báo...</p>}
        {query.error && (
          <div className={styles.state} role="alert">
            <p>{getApiErrorMessage(query.error, "Không thể tải thông báo.")}</p>
            <button type="button" onClick={() => query.refetch()}>Thử lại</button>
          </div>
        )}
        {!query.isLoading && !query.error && notifications.length === 0 && (
          <p className={styles.state}>Chưa có thông báo</p>
        )}
        {notifications.map((notification) => (
          <NotificationItem key={notification.id} notification={notification} compact onSelect={selectNotification} disabled={mutation.isPending} />
        ))}
      </div>
      <button type="button" className={styles.viewAll} onClick={() => { onClose(); navigate("/notifications"); }}>
        Xem tất cả
      </button>
    </section>
  );
}

export default NotificationDropdown;
