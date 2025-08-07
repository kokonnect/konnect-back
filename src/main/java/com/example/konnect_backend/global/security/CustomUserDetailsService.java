package com.example.konnect_backend.global.security;

import com.example.konnect_backend.domain.user.entity.User;
import com.example.konnect_backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findBySocialId(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with socialId: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getSocialId())
                .password("") // OAuth2 login doesn't use password
                .authorities(new ArrayList<>())
                .build();
    }
}