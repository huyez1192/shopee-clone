package com.ecommerce.minishopee.chat.realtime;

import com.ecommerce.minishopee.security.jwt.JwtService;
import com.ecommerce.minishopee.security.service.CustomUserDetailsService;
import com.ecommerce.minishopee.user.entity.AppUser;
import com.ecommerce.minishopee.user.enums.Role;
import com.ecommerce.minishopee.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StompJwtChannelInterceptorTests {
    @Mock JwtService jwtService;
    @Mock CustomUserDetailsService userDetailsService;
    private StompJwtChannelInterceptor interceptor;
    private AppUser user;

    @BeforeEach
    void setUp() {
        interceptor = new StompJwtChannelInterceptor(jwtService, userDetailsService);
        user = new AppUser(); user.setId(1L); user.setEmail("user@test.local"); user.setFullName("User");
        user.setRole(Role.USER); user.setStatus(UserStatus.ACTIVE);
    }

    @Test
    void validJwtConnectSetsAuthenticatedPrincipal() {
        when(jwtService.extractUsername("valid")).thenReturn(user.getUsername());
        when(userDetailsService.loadUserByUsername(user.getUsername())).thenReturn(user);
        when(jwtService.isTokenValid("valid", user)).thenReturn(true);
        Message<?> result = interceptor.preSend(message(StompCommand.CONNECT, null, "Bearer valid"), mock());
        StompHeaderAccessor resultAccessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertNotNull(resultAccessor);
        assertNotNull(resultAccessor.getUser());
        assertEquals(user.getUsername(), resultAccessor.getUser().getName());
    }

    @Test
    void missingAndInvalidJwtAreRejectedDuringConnect() {
        assertThrows(BadCredentialsException.class,
                () -> interceptor.preSend(message(StompCommand.CONNECT, null, null), mock()));
        when(jwtService.extractUsername("bad")).thenThrow(new IllegalArgumentException("invalid"));
        assertThrows(BadCredentialsException.class,
                () -> interceptor.preSend(message(StompCommand.CONNECT, null, "Bearer bad"), mock()));
    }

    @Test
    void expiredOrLockedAccountIsRejected() {
        when(jwtService.extractUsername("expired")).thenReturn(user.getUsername());
        when(userDetailsService.loadUserByUsername(user.getUsername())).thenReturn(user);
        when(jwtService.isTokenValid("expired", user)).thenReturn(false);
        assertThrows(BadCredentialsException.class,
                () -> interceptor.preSend(message(StompCommand.CONNECT, null, "Bearer expired"), mock()));

        user.setStatus(UserStatus.LOCKED);
        when(jwtService.extractUsername("locked")).thenReturn(user.getUsername());
        assertThrows(BadCredentialsException.class,
                () -> interceptor.preSend(message(StompCommand.CONNECT, null, "Bearer locked"), mock()));
    }

    @Test
    void authenticatedUserMaySubscribeToNotificationQueue() {
        Message<?> source = message(StompCommand.SUBSCRIBE, "/user/queue/notifications", null);
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(source, StompHeaderAccessor.class);
        accessor.setUser(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities()));
        assertDoesNotThrow(() -> interceptor.preSend(source, mock()));
    }

    private Message<?> message(StompCommand command, String destination, String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (destination != null) accessor.setDestination(destination);
        if (authorization != null) accessor.setNativeHeader("Authorization", authorization);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
