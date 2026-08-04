package com.ecommerce.minishopee.user.controller;

import com.ecommerce.minishopee.user.dto.request.ChangePasswordRequest;
import com.ecommerce.minishopee.user.dto.request.UpdateProfileRequest;
import com.ecommerce.minishopee.user.dto.response.UserResponse;
import com.ecommerce.minishopee.user.entity.AppUser;
import com.ecommerce.minishopee.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser(@AuthenticationPrincipal AppUser user) {
        // @AuthenticationPrincipal để lấy user hiện tại đang đăng nhập từ SecurityContext.
        // (Dòng SecurityContextHolder.getContext().setAuthentication(authenticationToken); trong JwtAuthenticationFilter)
        return userService.getCurrentUser(user);
    }

    @PutMapping("/me")
    public UserResponse updateProfile(@AuthenticationPrincipal AppUser user, @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(user, request);
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal AppUser user, @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(user,request);
    }
}
