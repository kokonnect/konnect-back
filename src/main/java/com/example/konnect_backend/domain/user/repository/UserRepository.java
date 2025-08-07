package com.example.konnect_backend.domain.user.repository;

import com.example.konnect_backend.domain.user.entity.User;
import com.example.konnect_backend.domain.user.entity.status.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findBySocialId(String socialId);
    
    Optional<User> findBySocialIdAndProvider(String socialId, Provider provider);
    
    boolean existsBySocialId(String socialId);
}