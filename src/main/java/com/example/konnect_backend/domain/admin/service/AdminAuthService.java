package com.example.konnect_backend.domain.admin.service;

import com.example.konnect_backend.domain.admin.dto.request.AdminLoginRequest;
import com.example.konnect_backend.domain.admin.entity.Admin;
import com.example.konnect_backend.domain.admin.repository.AdminRepository;
import com.example.konnect_backend.domain.auth.dto.response.AuthResponse;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.exception.GeneralException;
import com.example.konnect_backend.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAuthService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthResponse login(AdminLoginRequest request) {
        Admin admin = adminRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.PASSWORD_FAILED));

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new GeneralException(ErrorStatus.PASSWORD_FAILED);
        }

        String accessToken = jwtTokenProvider.createToken(admin.getId(), "ADMIN");
        return AuthResponse.of(accessToken, admin.getId(), "ADMIN");
    }
}
