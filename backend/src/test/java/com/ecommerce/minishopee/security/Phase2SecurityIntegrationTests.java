package com.ecommerce.minishopee.security;

import com.ecommerce.minishopee.security.jwt.JwtService;
import com.ecommerce.minishopee.notification.entity.Notification;
import com.ecommerce.minishopee.notification.enums.NotificationType;
import com.ecommerce.minishopee.notification.repository.NotificationRepository;
import com.ecommerce.minishopee.shop.entity.Shop;
import com.ecommerce.minishopee.shop.enums.ShopStatus;
import com.ecommerce.minishopee.shop.repository.ShopRepository;
import com.ecommerce.minishopee.user.entity.AppUser;
import com.ecommerce.minishopee.user.enums.Role;
import com.ecommerce.minishopee.user.enums.UserStatus;
import com.ecommerce.minishopee.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class Phase2SecurityIntegrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired AppUserRepository userRepository;
    @Autowired ShopRepository shopRepository;
    @Autowired JwtService jwtService;
    @Autowired NotificationRepository notificationRepository;

    private String userToken;
    private String sellerToken;
    private String adminToken;
    private AppUser sellerAccount;

    @BeforeEach
    void setUp() {
        AppUser user = user("phase2-user@example.test", Role.USER);
        AppUser seller = user("phase2-seller@example.test", Role.SELLER);
        AppUser admin = user("phase2-admin@example.test", Role.ADMIN);
        Shop shop = new Shop();
        shop.setOwner(seller); shop.setName("Phase 2 Security Shop"); shop.setAddress("Address");
        shop.setContactPhone("0123456789"); shop.setStatus(ShopStatus.APPROVED);
        shopRepository.saveAndFlush(shop);
        userToken = jwtService.generateToken(user);
        sellerToken = jwtService.generateToken(seller);
        adminToken = jwtService.generateToken(admin);
        sellerAccount = seller;
    }

    @Test
    void anonymousIsBlockedFromAllPhaseTwoPrivateEndpointGroups() throws Exception {
        mockMvc.perform(get("/api/notifications/unread-count")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/seller/statistics/overview").param("from", "2026-09-01").param("to", "2026-09-30"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/statistics/overview").param("from", "2026-09-01").param("to", "2026-09-30"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/reviews").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        MockMultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", new byte[]{1});
        mockMvc.perform(multipart("/api/uploads/images").file(file).param("purpose", "PRODUCT"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sellerCanUseSellerStatisticsReviewsNotificationsAndAuthenticatedUpload() throws Exception {
        mockMvc.perform(get("/api/seller/statistics/overview").with(bearer(sellerToken))
                        .param("from", "2026-09-01").param("to", "2026-09-30"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/seller/reviews").with(bearer(sellerToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/notifications/unread-count").with(bearer(sellerToken)))
                .andExpect(status().isOk());
        MockMultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", new byte[]{1});
        mockMvc.perform(multipart("/api/uploads/images").file(file).param("purpose", "PRODUCT")
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void sellerCannotUseAdminStatistics() throws Exception {
        mockMvc.perform(get("/api/admin/statistics/overview").with(bearer(sellerToken))
                        .param("from", "2026-09-01").param("to", "2026-09-30"))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCanUseAuthenticatedEndpointsButNotSellerOrAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/notifications/unread-count").with(bearer(userToken))).andExpect(status().isOk());
        mockMvc.perform(post("/api/reviews").with(bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/seller/statistics/overview").with(bearer(userToken))
                        .param("from", "2026-09-01").param("to", "2026-09-30"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/seller/reviews").with(bearer(userToken))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/statistics/overview").with(bearer(userToken))
                        .param("from", "2026-09-01").param("to", "2026-09-30"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanUseAdminNotificationsAndUploadButNotSellerStatistics() throws Exception {
        mockMvc.perform(get("/api/admin/statistics/overview").with(bearer(adminToken))
                        .param("from", "2026-09-01").param("to", "2026-09-30"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/notifications/unread-count").with(bearer(adminToken))).andExpect(status().isOk());
        MockMultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", new byte[]{1});
        mockMvc.perform(multipart("/api/uploads/images").file(file).param("purpose", "PRODUCT")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isServiceUnavailable());
        mockMvc.perform(get("/api/seller/statistics/overview").with(bearer(adminToken))
                        .param("from", "2026-09-01").param("to", "2026-09-30"))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotMarkAnotherUsersNotificationThroughHttp() throws Exception {
        Notification notification = new Notification(); notification.setRecipient(sellerAccount);
        notification.setType(NotificationType.NEW_ORDER); notification.setTitle("Title");
        notification.setMessage("Message"); notification.setTargetUrl("/seller/orders/1");
        notification = notificationRepository.saveAndFlush(notification);

        mockMvc.perform(patch("/api/notifications/{id}/read", notification.getId()).with(bearer(userToken)))
                .andExpect(status().isNotFound());
        assertFalse(notificationRepository.findById(notification.getId()).orElseThrow().isRead());
    }

    private AppUser user(String email, Role role) {
        AppUser user = new AppUser(); user.setFullName(role.name()); user.setEmail(email);
        user.setPassword("password"); user.setPhone(email); user.setRole(role); user.setStatus(UserStatus.ACTIVE);
        return userRepository.saveAndFlush(user);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor bearer(String token) {
        return request -> { request.addHeader("Authorization", "Bearer " + token); return request; };
    }
}
