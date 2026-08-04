package com.ecommerce.minishopee.controller;

import com.ecommerce.minishopee.dto.ChangePasswordRequest;
import com.ecommerce.minishopee.dto.UpdateProfileRequest;
import com.ecommerce.minishopee.dto.UserResponse;
import com.ecommerce.minishopee.entity.AppUser;
import com.ecommerce.minishopee.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
