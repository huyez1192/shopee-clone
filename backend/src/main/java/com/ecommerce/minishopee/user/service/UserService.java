package com.ecommerce.minishopee.user.service;

import com.ecommerce.minishopee.user.dto.request.ChangePasswordRequest;
import com.ecommerce.minishopee.user.dto.request.UpdateProfileRequest;
import com.ecommerce.minishopee.user.dto.response.UserResponse;
import com.ecommerce.minishopee.user.entity.AppUser;
import com.ecommerce.minishopee.common.exception.BadRequestException;
import com.ecommerce.minishopee.user.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse getCurrentUser(AppUser user) {
        return toResponse(user);
    }

    public UserResponse updateProfile(AppUser currentUser, UpdateProfileRequest request) {
        currentUser.setFullName(request.getFullName().trim());
        currentUser.setPhone(request.getPhone().trim());
        currentUser.setAvatarUrl(request.getAvatarUrl());

        AppUser updatedUser = appUserRepository.save(currentUser);
        return toResponse(updatedUser);
    }

    public void changePassword(AppUser currentUser, ChangePasswordRequest request) {

        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            throw new BadRequestException("Mật khẩu hiện tại không chính xác.");
        }

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new BadRequestException("Mật khẩu mới và xác nhận mật khẩu không khớp.");
        }

        if (passwordEncoder.matches(request.getNewPassword(), currentUser.getPassword())) {
            throw new BadRequestException("Mật khẩu mới không được trùng với mật khẩu hiện tại.");
        }

        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));

        appUserRepository.save(currentUser);
    }


    public UserResponse toResponse(AppUser user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

}
