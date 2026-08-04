package com.ecommerce.minishopee.service;

import com.ecommerce.minishopee.dto.AuthResponse;
import com.ecommerce.minishopee.dto.LoginRequest;
import com.ecommerce.minishopee.dto.RegisterRequest;
import com.ecommerce.minishopee.entity.AppUser;
import com.ecommerce.minishopee.entity.UserStatus;
import com.ecommerce.minishopee.exception.BadRequestException;
import com.ecommerce.minishopee.repository.AppUserRepository;
import com.ecommerce.minishopee.security.Role;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    public AuthService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (appUserRepository.existsByEmail(email)) {
            throw new BadRequestException("Email này đã được sử dụng.");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu và xác nhận mật khẩu không khớp.");
        }

        AppUser user = new AppUser();

        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPhone(request.getPhone().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);

        AppUser savedUser = appUserRepository.save(user);

        String token = jwtService.generateToken(savedUser);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        request.getPassword()
                )
        );

        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Email hoặc password không hợp lệ."));

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
