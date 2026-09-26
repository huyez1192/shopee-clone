import { useEffect, useMemo, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate, useParams } from "react-router-dom";

import LoadingState from "../../../components/common/LoadingState";
import ErrorState from "../../../components/common/ErrorState";
import EmptyState from "../../../components/common/EmptyState";
import Pagination from "../../../components/common/Pagination";
import useAuth from "../../auth/hooks/useAuth";
import useRealtime from "../../realtime/useRealtime";
import { queryKeys } from "../../../query/queryKeys";
import { getApiErrorMessage } from "../../../utils/getApiErrorMessage";
import {
  getConversation,
  getCustomerConversations,
  getMessages,
  getSellerConversations,
  markConversationRead,
  sendMessage,
} from "../api/chatApi";
import { addMessageToPage } from "../realtime/chatCache";

import styles from "./ChatWorkspace.module.css";

const LIST_SIZE = 20;
const MESSAGE_SIZE = 30;

function formatTime(value) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit",
  }).format(date);
}

function ChatWorkspace({ context }) {
  const sellerMode = context === "SELLER";
  const basePath = sellerMode ? "/seller/chat" : "/chat";
  const { conversationId } = useParams();
  const activeId = conversationId ? Number(conversationId) : null;
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const { connected } = useRealtime();
  const [listPage, setListPage] = useState(1);
  const [messagePage, setMessagePage] = useState(1);
  const [draft, setDraft] = useState("");
  const lastMarkedRef = useRef("");
  const messagesRef = useRef(null);
  const shouldStickToBottomRef = useRef(true);
  const listParams = useMemo(() => ({ page: listPage - 1, size: LIST_SIZE }), [listPage]);
  const messageParams = useMemo(() => ({ page: messagePage - 1, size: MESSAGE_SIZE }), [messagePage]);
  const listKey = sellerMode
    ? queryKeys.chat.sellerConversations(listParams)
    : queryKeys.chat.customerConversations(listParams);
  const listRoot = sellerMode
    ? queryKeys.chat.sellerConversationsRoot
    : queryKeys.chat.customerConversationsRoot;
  const unreadKey = sellerMode ? queryKeys.chat.sellerUnread() : queryKeys.chat.customerUnread();

  const listQuery = useQuery({
    queryKey: listKey,
    queryFn: () => sellerMode
      ? getSellerConversations(listParams)
      : getCustomerConversations(listParams),
  });
  const detailQuery = useQuery({
    queryKey: queryKeys.chat.conversation(activeId),
    queryFn: () => getConversation(activeId),
    enabled: Boolean(activeId),
  });
  const messagesQuery = useQuery({
    queryKey: queryKeys.chat.messages(activeId, messageParams),
    queryFn: () => getMessages(activeId, messageParams),
    enabled: Boolean(activeId),
  });

  const markReadMutation = useMutation({
    mutationFn: () => markConversationRead(activeId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: listRoot });
      queryClient.invalidateQueries({ queryKey: unreadKey });
    },
  });
  const sendMutation = useMutation({
    mutationFn: (content) => sendMessage(activeId, content),
    onSuccess: (message) => {
      shouldStickToBottomRef.current = true;
      const pageZeroKey = queryKeys.chat.messages(activeId, { page: 0, size: MESSAGE_SIZE });
      queryClient.setQueryData(pageZeroKey, (page) => addMessageToPage(page, message, user.id));
      queryClient.invalidateQueries({ queryKey: listRoot });
      setDraft("");
      setMessagePage(1);
    },
  });

  useEffect(() => {
    if (!activeId || !messagesQuery.data?.content?.length) return;
    const newest = messagesQuery.data.content[0];
    if (newest.mine) return;
    const marker = `${activeId}:${newest.id}`;
    if (lastMarkedRef.current === marker) return;
    lastMarkedRef.current = marker;
    markReadMutation.mutate();
  }, [activeId, markReadMutation, messagesQuery.data]);

  function submitMessage(event) {
    event.preventDefault();
    const content = draft.trim();
    if (!content || sendMutation.isPending) return;
    sendMutation.mutate(content);
  }

  const conversations = listQuery.data?.content || [];
  const chronologicalMessages = [...(messagesQuery.data?.content || [])].sort((left, right) => {
    const time = new Date(left.createdAt).getTime() - new Date(right.createdAt).getTime();
    return time || Number(left.id) - Number(right.id);
  });
  const newestRenderedMessageId = chronologicalMessages.at(-1)?.id;

  useEffect(() => {
    shouldStickToBottomRef.current = true;
  }, [activeId]);

  useEffect(() => {
    if (messagePage !== 1 || !shouldStickToBottomRef.current || !messagesRef.current) return;
    messagesRef.current.scrollTo({ top: messagesRef.current.scrollHeight, behavior: "smooth" });
  }, [activeId, messagePage, newestRenderedMessageId]);
  const counterpart = detailQuery.data
    ? (sellerMode ? detailQuery.data.customer : detailQuery.data.shop)
    : null;

  return (
    <section className={styles.page}>
      <header className={styles.pageHeader}>
        <div><h1>Tin nhắn</h1></div>
        {!connected && <span className={styles.reconnecting}>Đang kết nối lại...</span>}
      </header>

      <div className={`${styles.workspace} ${activeId ? styles.threadSelected : ""}`}>
        <aside className={styles.conversationPane}>
          <h2>{sellerMode ? "Hộp thư Shop" : "Các Shop"}</h2>
          {listQuery.isLoading && <LoadingState message="Đang tải cuộc trò chuyện..." />}
          {listQuery.error && <ErrorState message={getApiErrorMessage(listQuery.error)} onRetry={listQuery.refetch} />}
          {!listQuery.isLoading && !listQuery.error && conversations.length === 0 && (
            <EmptyState title="Chưa có cuộc trò chuyện" description="Tin nhắn mới sẽ xuất hiện tại đây." />
          )}
          <div className={styles.conversationList}>
            {conversations.map((conversation) => {
              const person = sellerMode ? conversation.customer : conversation.shop;
              return (
                <button
                  type="button"
                  key={conversation.conversationId}
                  className={`${styles.conversationItem} ${activeId === conversation.conversationId ? styles.active : ""}`}
                  onClick={() => {
                    setMessagePage(1);
                    navigate(`${basePath}/${conversation.conversationId}`);
                  }}
                >
                  <span className={styles.avatar}>
                    {person.avatarUrl || person.logoUrl
                      ? <img src={person.avatarUrl || person.logoUrl} alt="" />
                      : (person.displayName || person.name || "?").slice(0, 1).toUpperCase()}
                  </span>
                  <span className={styles.conversationText}>
                    <strong>{person.displayName || person.name}</strong>
                    <small>{conversation.lastMessageContent || "Chưa có tin nhắn"}</small>
                  </span>
                  <span className={styles.meta}>
                    <time>{formatTime(conversation.lastMessageAt || conversation.createdAt)}</time>
                    {conversation.unreadCount > 0 && <b>{conversation.unreadCount > 99 ? "99+" : conversation.unreadCount}</b>}
                  </span>
                </button>
              );
            })}
          </div>
          <Pagination currentPage={listPage} totalPages={listQuery.data?.totalPages || 0} onPageChange={setListPage} />
        </aside>

        <main className={styles.threadPane}>
          {!activeId && <div className={styles.noThread}><EmptyState title="Chọn một cuộc trò chuyện" description="Chọn hội thoại bên trái để xem và gửi tin nhắn." /></div>}
          {activeId && detailQuery.isLoading && <LoadingState message="Đang tải cuộc trò chuyện..." />}
          {activeId && detailQuery.error && <ErrorState message={getApiErrorMessage(detailQuery.error)} onRetry={detailQuery.refetch} />}
          {activeId && detailQuery.data && (
            <>
              <header className={styles.threadHeader}>
                <button type="button" onClick={() => navigate(basePath)} aria-label="Quay lại danh sách">‹</button>
                <strong>{counterpart?.displayName || counterpart?.name}</strong>
              </header>
              <div
                ref={messagesRef}
                className={styles.messages}
                onScroll={(event) => {
                  const element = event.currentTarget;
                  shouldStickToBottomRef.current = element.scrollHeight - element.scrollTop - element.clientHeight < 80;
                }}
              >
                {messagesQuery.isLoading && <LoadingState message="Đang tải tin nhắn..." />}
                {messagesQuery.error && <ErrorState message={getApiErrorMessage(messagesQuery.error)} onRetry={messagesQuery.refetch} />}
                {!messagesQuery.isLoading && !messagesQuery.error && chronologicalMessages.length === 0 && (
                  <p className={styles.emptyMessages}>Hãy gửi lời chào để bắt đầu cuộc trò chuyện.</p>
                )}
                {chronologicalMessages.map((message) => (
                  <article key={message.id} className={`${styles.message} ${message.mine ? styles.mine : styles.theirs}`}>
                    <p>{message.content}</p><time>{formatTime(message.createdAt)}</time>
                  </article>
                ))}
              </div>
              <Pagination currentPage={messagePage} totalPages={messagesQuery.data?.totalPages || 0} onPageChange={setMessagePage} />
              <form className={styles.composer} onSubmit={submitMessage}>
                <textarea
                  value={draft}
                  maxLength={2000}
                  placeholder="Nhập tin nhắn..."
                  onChange={(event) => setDraft(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === "Enter" && !event.shiftKey) { event.preventDefault(); submitMessage(event); }
                  }}
                />
                <button type="submit" disabled={!draft.trim() || sendMutation.isPending}>Gửi</button>
              </form>
              {sendMutation.error && <p className={styles.sendError}>{getApiErrorMessage(sendMutation.error, "Không thể gửi tin nhắn.")}</p>}
            </>
          )}
        </main>
      </div>
    </section>
  );
}

export default ChatWorkspace;
