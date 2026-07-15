package com.example.productcrudapi.controller;

import com.example.productcrudapi.entity.AppUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class UserController {

    @GetMapping("/api/users/me")
    public Map<String, Object> getCurrentUser(@AuthenticationPrincipal AppUser user) {
        // @AuthenticationPrincipal để lấy user hiện tại đang đăng nhập từ SecurityContext.
        // (Dòng SecurityContextHolder.getContext().setAuthentication(authenticationToken); trong JwtAuthenticationFilter)
        return Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "role", user.getRole()
        );
    }
}
