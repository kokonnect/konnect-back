// src/main/java/com/example/konnect_backend/domain/user/repository/ChildRepository.java
package com.example.konnect_backend.domain.user.repository;

import com.example.konnect_backend.domain.user.entity.Child;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChildRepository extends JpaRepository<Child, Long> {
}
