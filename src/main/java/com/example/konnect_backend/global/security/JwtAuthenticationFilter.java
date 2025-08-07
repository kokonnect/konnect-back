package com.example.konnect_backend.global.security;

import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.exception.GeneralException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    private TokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = parseBearerToken(request);

            if (token == null || token.equalsIgnoreCase("null")) {
                filterChain.doFilter(request, response);
                return;
            }

            Long userId = tokenProvider.extractUserIdFromToken(token);
            log.info("Authenticated user ID: " + userId);

            AbstractAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, AuthorityUtils.NO_AUTHORITIES);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                securityContext.setAuthentication(authentication);
                SecurityContextHolder.setContext(securityContext);


            filterChain.doFilter(request, response);
        } catch (GeneralException e) {
            handleException(response, e); // 예외 발생 시 직접 JSON 응답 반환
        } catch (Exception e) {
            handleException(response, new GeneralException(ErrorStatus.JWT_MALFORMED));
        }
    }

    private String parseBearerToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private void handleException(HttpServletResponse response, GeneralException ex) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(ex.getErrorReasonHttpStatus().getHttpStatus().value());
        response.getWriter().write(new ObjectMapper().writeValueAsString(Map.of(
                "status", ex.getErrorReasonHttpStatus().getCode(),
                "message", ex.getErrorReasonHttpStatus().getMessage()
        )));
    }
}
