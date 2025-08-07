
package com.example.konnect_backend.global.security;


import com.example.konnect_backend.domain.user.entity.User;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.exception.GeneralException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class TokenProvider {

    private static final SecretKey SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS512); // 토큰용 비밀키

    public String generateAccessToken(User user) {
        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("userId", user.getId())
                .setExpiration(Date.from(Instant.now().plusSeconds(900))) // 15분
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }


    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .setSubject(user.getId().toString())
                .setExpiration(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    @Deprecated
    public Claims parseToken(String token) {
        return extractClaims(token);
    }



    // 토큰 검증 및 클레임 추출
    public Claims extractClaims(String token) {
        try {
            String jwtToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            return Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(jwtToken)
                    .getBody();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰에서도 클레임 추출
            return e.getClaims();
        }
    }

    // 사용자명 추출
    public String extractUsernameFromToken(String token) {
        return extractClaims(token).getSubject(); // JWT의 subject에서 사용자명 추출
    }

    // 사용자 ID 추출
    public Long extractUserIdFromToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("토큰이 비어 있거나 null입니다.");
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        try {
            return extractClaims(token).get("userId", Long.class);
        } catch (Exception e) {
            throw new GeneralException(ErrorStatus.JWT_MALFORMED);
        }
    }




    // 토큰 유효성 확인
    public boolean isValidToken(String token) {
        try {
            String jwtToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(jwtToken);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}