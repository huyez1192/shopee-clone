package com.ecommerce.minishopee.chat.realtime;

import com.ecommerce.minishopee.security.jwt.JwtService;
import com.ecommerce.minishopee.security.service.CustomUserDetailsService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class StompJwtChannelInterceptor implements ChannelInterceptor {
    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer ";
    private static final String CHAT_SUBSCRIPTION = "/user/queue/chat/events";
    private static final String NOTIFICATION_SUBSCRIPTION = "/user/queue/notifications";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public StompJwtChannelInterceptor(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        if (command == StompCommand.CONNECT) authenticate(accessor);
        if (command == StompCommand.SUBSCRIBE) authorizeSubscription(accessor);
        if (command == StompCommand.SEND) throw new AccessDeniedException("WebSocket chat chỉ hỗ trợ server push.");
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader(AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER) || header.length() == BEARER.length()) {
            throw new BadCredentialsException("Thiếu JWT cho kết nối WebSocket.");
        }
        try {
            String token = header.substring(BEARER.length());
            String username = jwtService.extractUsername(token);
            UserDetails user = userDetailsService.loadUserByUsername(username);
            if (!user.isEnabled() || !user.isAccountNonLocked() || !jwtService.isTokenValid(token, user)) {
                throw new BadCredentialsException("JWT WebSocket không hợp lệ.");
            }
            accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        } catch (BadCredentialsException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadCredentialsException("JWT WebSocket không hợp lệ.", exception);
        }
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) throw new BadCredentialsException("WebSocket chưa được xác thực.");
        if (!CHAT_SUBSCRIPTION.equals(accessor.getDestination())
                && !NOTIFICATION_SUBSCRIPTION.equals(accessor.getDestination())) {
            throw new AccessDeniedException("STOMP destination không được phép.");
        }
    }
}
