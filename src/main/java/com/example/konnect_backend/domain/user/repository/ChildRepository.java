// src/main/java/com/example/konnect_backend/domain/user/repository/ChildRepository.java
package com.example.konnect_backend.domain.user.repository;

import com.example.konnect_backend.domain.user.entity.Child;
import com.example.konnect_backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChildRepository extends JpaRepository<Child, Long> {
    @Modifying
    @Query("UPDATE Child c SET c.user = :to WHERE c.user = :from")
    int reassignOwner(User from, User to);

    List<Child> findByUser(User user);
}

