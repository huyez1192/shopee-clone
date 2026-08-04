package com.ecommerce.minishopee.user.admin.service;

import com.ecommerce.minishopee.user.admin.dto.request.AdminLockUserRequest;
import com.ecommerce.minishopee.user.admin.dto.response.AdminUserResponse;
import com.ecommerce.minishopee.user.entity.AppUser;
import com.ecommerce.minishopee.shop.entity.Shop;
import com.ecommerce.minishopee.user.enums.UserStatus;
import com.ecommerce.minishopee.common.exception.BadRequestException;
import com.ecommerce.minishopee.user.exception.UserNotFoundException;
import com.ecommerce.minishopee.user.admin.mapper.AdminUserMapper;
import com.ecommerce.minishopee.user.repository.AppUserRepository;
import com.ecommerce.minishopee.shop.repository.ShopRepository;
import com.ecommerce.minishopee.user.enums.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminUserService {

    private final AppUserRepository appUserRepository;
    private final ShopRepository shopRepository;

    public AdminUserService(AppUserRepository appUserRepository, ShopRepository shopRepository
    ) {
        this.appUserRepository = appUserRepository;
        this.shopRepository = shopRepository;
    }

    // Admin tìm kiếm và lọc User.
    @Transactional(readOnly = true)
    public List<AdminUserResponse> getUsers(
            String keyword,
            Role role,
            UserStatus status
    ) {
        String normalizedKeyword = keyword == null || keyword.isBlank()
                                    ? ""
                                    : keyword.trim().toLowerCase();

        List<AppUser> users = appUserRepository.searchUsersForAdmin(
                                                        normalizedKeyword,
                                                        role,
                                                        status
                                                );
        return toResponses(users);
    }

    // Admin xem chi tiết User.
    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(Long userId) {
        AppUser user = appUserRepository.findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));

        Shop shop = shopRepository.findByOwnerId(userId).orElse(null);

        return AdminUserMapper.toResponse(user, shop);
    }

    // Admin khóa tài khoản.
    @Transactional
    public AdminUserResponse lockUser(
            Long userId,
            AppUser currentAdmin,
            AdminLockUserRequest request
    ) {
        AppUser targetUser = appUserRepository.findByIdForUpdate(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));

        validateCanManageUser(targetUser, currentAdmin);

        if (targetUser.getStatus() == UserStatus.LOCKED) {
            throw new BadRequestException("Tài khoản đã bị khóa trước đó.");
        }

        targetUser.setStatus(UserStatus.LOCKED);

        targetUser.setLockedReason(request.getReason().trim());

        targetUser.setLockedAt(LocalDateTime.now());

        targetUser.setLockedByAdminId(currentAdmin.getId());

        AppUser savedUser = appUserRepository.save(targetUser);

        Shop shop = shopRepository.findByOwnerId(savedUser.getId())
                                .orElse(null);

        return AdminUserMapper.toResponse(savedUser, shop);
    }

    // Admin mở khóa tài khoản.
    @Transactional
    public AdminUserResponse unlockUser(Long userId, AppUser currentAdmin) {
        AppUser targetUser = appUserRepository.findByIdForUpdate(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));

        validateCanManageUser(targetUser, currentAdmin);

        if (targetUser.getStatus() == UserStatus.ACTIVE) {
            throw new BadRequestException("Tài khoản hiện không bị khóa.");
        }

        targetUser.setStatus(UserStatus.ACTIVE);

        /*
         * Đây là thông tin trạng thái khóa hiện tại, nên xóa khi mở khóa.
         *
         * Sau này có thể tạo bảng audit riêng để lưu lịch sử khóa/mở khóa.
         */
        targetUser.setLockedReason(null);
        targetUser.setLockedAt(null);
        targetUser.setLockedByAdminId(null);

        AppUser savedUser = appUserRepository.save(targetUser);

        Shop shop = shopRepository.findByOwnerId(savedUser.getId())
                                .orElse(null);

        return AdminUserMapper.toResponse(savedUser, shop);
    }

    private void validateCanManageUser(AppUser targetUser, AppUser currentAdmin) {
        // Không cho Admin tự khóa chính mình.
        if (targetUser.getId().equals(currentAdmin.getId())) {
            throw new BadRequestException("Admin không thể tự khóa tài khoản của mình.");
        }

        // Hệ thống hiện chưa có phân cấp Admin, nên không cho Admin khóa Admin khác.
        if (targetUser.getRole() == Role.ADMIN) {
            throw new BadRequestException("Không thể khóa tài khoản ADMIN.");
        }
    }

    private List<AdminUserResponse> toResponses(List<AppUser> users) {
        if (users.isEmpty()) {
            return List.of();
        }

        List<Long> userIds = users.stream()
                                .map(AppUser::getId)
                                .toList();

        Map<Long, Shop> shopByOwnerId = shopRepository
                        .findAllByOwner_IdIn(userIds)
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        shop -> shop.getOwner().getId(),
                                        Function.identity(),
                                        (first, second) -> first
                                )
                        );

        return users.stream()
                .map(user -> AdminUserMapper.toResponse(user, shopByOwnerId.get(user.getId())))
                .toList();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}