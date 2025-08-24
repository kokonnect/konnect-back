package com.example.konnect_backend.domain.document.repository;

import com.example.konnect_backend.domain.document.entity.DocumentFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentFileRepository extends JpaRepository<DocumentFile, Long> {
    
    List<DocumentFile> findByDocumentId(Long documentId);
}