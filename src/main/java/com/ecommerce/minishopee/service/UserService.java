package com.ecommerce.minishopee.service;

import com.ecommerce.minishopee.dto.ChangePasswordRequest;
import com.ecommerce.minishopee.dto.UpdateProfileRequest;
import com.ecommerce.minishopee.dto.UserResponse;
import com.ecommerce.minishopee.entity.AppUser;
import com.ecommerce.minishopee.exception.BadRequestException;
import com.ecommerce.minishopee.repository.AppUserRepository;
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
