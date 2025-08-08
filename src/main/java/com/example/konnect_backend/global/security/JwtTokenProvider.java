package com.example.konnect_backend.global.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long validityInMilliseconds;

    public JwtTokenProvider(
            @Value("${jwt.secret:defaultSecretKeyForDevelopmentPurposeOnly12345678}") String secretKey,
            @Value("${jwt.token-validity-in-seconds:86400}") long validityInSeconds
    ) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.validityInMilliseconds = validityInSeconds * 1000;
    }

    /** ✅ 새 방식: userId + role(GUEST/USER) 을 토큰에 넣기 */
    public String createToken(Long userId, String role) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))   // sub = userId
                .claim("role", role)                  // "GUEST" or "USER"
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** ❌ (구방식) socialId 기반 생성 — 점진 이행 중이면 임시 유지 가능 */
    @Deprecated
    public String createToken(String socialId) {
        // 필요하면 내부적으로 USER 권한 부여 로직을 넣어도 됨
        return createToken(Long.valueOf(socialId), "USER"); // 진짜 socialId가 숫자가 아니라면 쓰지마!
    }

    /** ✅ 토큰 → Authentication (UserDetailsService 불필요) */
    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        String sub = claims.getSubject();              // userId
        String role = claims.get("role", String.class);
        return new UsernamePasswordAuthenticationToken(
                sub,                                   // principal = userId 문자열
                "",                                    // credentials
                List.of(new SimpleGrantedAuthority("ROLE_" + role)) // ROLE_GUEST/ROLE_USER
        );
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
