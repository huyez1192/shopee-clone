import { useEffect, useMemo, useState } from "react";
import { Client } from "@stomp/stompjs";
import { useQueryClient } from "@tanstack/react-query";

import useAuth from "../auth/hooks/useAuth";
import { queryKeys } from "../../query/queryKeys";
import { addMessageToPage } from "../chat/realtime/chatCache";
import RealtimeContext from "./RealtimeContext";

function getWebSocketUrl() {
  if (import.meta.env.VITE_WS_URL) return import.meta.env.VITE_WS_URL;
  const apiUrl = new URL(import.meta.env.VITE_API_BASE_URL);
  const protocol = apiUrl.protocol === "https:" ? "wss:" : "ws:";
  return `${protocol}//${apiUrl.host}/ws`;
}

function handleNotificationEvent(event, queryClient) {
  if (event?.type !== "NOTIFICATION_CREATED" || !event.notification?.id) return;
  queryClient.invalidateQueries({ queryKey: queryKeys.notifications.all });
}

function RealtimeProvider({ children }) {
  const { token, user, isAuthenticated, isInitializing } = useAuth();
  const queryClient = useQueryClient();
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    if (isInitializing || !isAuthenticated || !token || !user?.id) return undefined;

    function invalidateContext(context) {
      if (context === "SELLER") {
        queryClient.invalidateQueries({ queryKey: queryKeys.chat.sellerConversationsRoot });
        queryClient.invalidateQueries({ queryKey: queryKeys.chat.sellerUnread() });
      } else {
        queryClient.invalidateQueries({ queryKey: queryKeys.chat.customerConversationsRoot });
        queryClient.invalidateQueries({ queryKey: queryKeys.chat.customerUnread() });
      }
    }

    function handleEvent(event) {
      if (!event?.type || !event?.conversationId) return;
      if (event.type === "CHAT_MESSAGE_CREATED" && event.message) {
        queryClient.getQueriesData({ queryKey: queryKeys.chat.messagesRoot(event.conversationId) })
          .forEach(([key, page]) => {
            if (key[3]?.page === 0) {
              queryClient.setQueryData(key, addMessageToPage(page, event.message, user.id));
            }
          });
      }
      invalidateContext(event.recipientContext);
      if (event.type === "CHAT_READ_UPDATED") {
        queryClient.invalidateQueries({ queryKey: queryKeys.chat.messagesRoot(event.conversationId) });
      }
    }

    const client = new Client({
      brokerURL: getWebSocketUrl(),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 4000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        setConnected(true);
        client.subscribe("/user/queue/chat/events", (frame) => {
          try {
            handleEvent(JSON.parse(frame.body));
          } catch (error) {
            console.error("Không thể xử lý sự kiện chat realtime.", error);
          }
        });
        client.subscribe("/user/queue/notifications", (frame) => {
          try {
            handleNotificationEvent(JSON.parse(frame.body), queryClient);
          } catch (error) {
            console.error("KhÃ´ng thá»ƒ xá»­ lÃ½ sá»± kiá»‡n notification realtime.", error);
          }
        });
        queryClient.invalidateQueries({ queryKey: queryKeys.chat.customerConversationsRoot });
        queryClient.invalidateQueries({ queryKey: queryKeys.chat.customerUnread() });
        queryClient.invalidateQueries({ queryKey: queryKeys.chat.messagesAll });
        if (user.role === "SELLER") {
          queryClient.invalidateQueries({ queryKey: queryKeys.chat.sellerConversationsRoot });
          queryClient.invalidateQueries({ queryKey: queryKeys.chat.sellerUnread() });
        }
        queryClient.invalidateQueries({ queryKey: queryKeys.notifications.all });
      },
      onDisconnect: () => setConnected(false),
      onWebSocketClose: () => setConnected(false),
      onStompError: () => setConnected(false),
    });
    client.activate();
    return () => {
      setConnected(false);
      void client.deactivate();
    };
  }, [isAuthenticated, isInitializing, queryClient, token, user?.id, user?.role]);

  const value = useMemo(() => ({ connected }), [connected]);
  return <RealtimeContext.Provider value={value}>{children}</RealtimeContext.Provider>;
}

export default RealtimeProvider;
