import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";

import EmptyState from "../../../components/common/EmptyState";
import ErrorState from "../../../components/common/ErrorState";
import LoadingState from "../../../components/common/LoadingState";
import Pagination from "../../../components/common/Pagination";
import { queryKeys } from "../../../query/queryKeys";
import { getApiErrorMessage } from "../../../utils/getApiErrorMessage";
import { getNotifications, markAllNotificationsRead, markNotificationRead } from "../api/notificationApi";
import NotificationItem from "../components/NotificationItem";
import { isSafeInternalTarget } from "../utils/notificationUtils";

import styles from "./NotificationPage.module.css";

const PAGE_SIZE = 10;
const FILTERS = [
  { value: "all", label: "Tất cả" },
  { value: "unread", label: "Chưa đọc" },
  { value: "read", label: "Đã đọc" },
];

function NotificationPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(1);
  const [filter, setFilter] = useState("all");
  const params = {
    page: page - 1,
    size: PAGE_SIZE,
    ...(filter === "unread" ? { read: false } : {}),
    ...(filter === "read" ? { read: true } : {}),
  };
  const query = useQuery({
    queryKey: queryKeys.notifications.list(params),
    queryFn: () => getNotifications(params),
  });
  const readMutation = useMutation({
    mutationFn: markNotificationRead,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.notifications.all }),
  });
  const readAllMutation = useMutation({
    mutationFn: markAllNotificationsRead,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.notifications.all }),
  });

  async function selectNotification(notification) {
    if (!notification.isRead) {
      try {
        await readMutation.mutateAsync(notification.id);
      } catch {
        return;
      }
    }
    if (isSafeInternalTarget(notification.targetUrl)) navigate(notification.targetUrl.trim());
  }

  function changeFilter(value) {
    setFilter(value);
    setPage(1);
  }

  const notifications = query.data?.content || [];

  return (
    <main className={styles.page}>
      <section className={styles.panel}>
        <header className={styles.header}>
          <div>
            <h1>Thông Báo</h1>
            <nav className={styles.filters} aria-label="Lọc thông báo">
              {FILTERS.map((item) => (
                <button key={item.value} type="button" className={filter === item.value ? styles.active : ""} onClick={() => changeFilter(item.value)}>
                  {item.label}
                </button>
              ))}
            </nav>
          </div>
          <button type="button" className={styles.readAll} disabled={readAllMutation.isPending} onClick={() => readAllMutation.mutate()}>
            {readAllMutation.isPending ? "Đang xử lý..." : "Đánh dấu Đã đọc tất cả"}
          </button>
        </header>

        {readAllMutation.error && <p className={styles.mutationError} role="alert">{getApiErrorMessage(readAllMutation.error, "Không thể đánh dấu tất cả đã đọc.")}</p>}
        {readMutation.error && <p className={styles.mutationError} role="alert">{getApiErrorMessage(readMutation.error, "Không thể đánh dấu thông báo đã đọc.")}</p>}
        {query.isLoading ? (
          <div className={styles.state}><LoadingState message="Đang tải thông báo..." /></div>
        ) : query.error ? (
          <div className={styles.state}><ErrorState message={getApiErrorMessage(query.error, "Không thể tải thông báo.")} onRetry={() => query.refetch()} /></div>
        ) : notifications.length === 0 ? (
          <div className={styles.state}><EmptyState title="Chưa có thông báo" description={filter === "all" ? "Các cập nhật mới sẽ xuất hiện tại đây." : "Không có thông báo phù hợp với bộ lọc này."} /></div>
        ) : (
          <div className={styles.list}>
            {notifications.map((notification) => (
              <NotificationItem key={notification.id} notification={notification} onSelect={selectNotification} disabled={readMutation.isPending} />
            ))}
          </div>
        )}
        <Pagination currentPage={page} totalPages={query.data?.totalPages || 0} onPageChange={setPage} />
      </section>
    </main>
  );
}

export default NotificationPage;
