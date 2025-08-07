package com.example.konnect_backend.global.config;

import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Map;

@Configuration
@EnableMethodSecurity
@EnableWebSecurity
public class WebSecurityConfig {

    private final ObjectMapper objectMapper;

    @Autowired
    public WebSecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        http.httpBasic(basic -> basic.disable());
        http.headers(headers -> headers.frameOptions(frame -> frame.disable()).disable());
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
//        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))


        // Swagger 경로 인증 비활성화
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/static/**", "/favicon.ico").permitAll()
                        .requestMatchers("/swagger", "/swagger/", "/swagger-ui/**", "/v3/api-docs/**").permitAll() // Swagger 허용
                        //.requestMatchers("/api/user/**", "/api/booths/**", "/api/**").permitAll()   // /api 이하 경로 접근 허용\
                        .requestMatchers("/api/**").permitAll()
                        .requestMatchers("/login", "/oauth2/**").permitAll()
                        .requestMatchers("/api/ws", "/api/ws/**", "/api/ws/info/**").permitAll()
                        .requestMatchers("/ws", "/ws/**", "/ws/info/**").permitAll()  // WebSocket 엔드포인트 허용
                        .requestMatchers("/favicon.ico").permitAll()
                        .anyRequest().authenticated() // 나머지 요청은 인증 필요
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        http.exceptionHandling(except -> {
            // 인증 실패 (401)
            except.authenticationEntryPoint((request, response, authException) -> {
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                        "status", ErrorStatus._UNAUTHORIZED.getCode(),
                        "message", ErrorStatus._UNAUTHORIZED.getMessage()
                )));
            });

            // 인가 실패 (403)
            except.accessDeniedHandler((request, response, accessDeniedException) -> {
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                        "status", ErrorStatus._FORBIDDEN.getCode(),
                        "message", ErrorStatus._FORBIDDEN.getMessage()
                )));
            });
        });


        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        final long MAX_AGE_SECS = 3600;
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        List<String> allowedOrigins = List.of(
                "http://localhost:8080",
                "http://localhost:5173",
                "ws://localhost:8080",
                "http://13.209.210.46:8080",
                "ws://13.209.210.46:8080",
                "http://localhost:3000",
                "https://financeus.netlify.app",
                "https://financeusapi.shop"
        );

        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);
        source.registerCorsConfiguration("/**", config);

        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
