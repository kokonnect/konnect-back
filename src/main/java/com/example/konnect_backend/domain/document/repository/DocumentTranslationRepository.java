package com.example.konnect_backend.domain.document.repository;

import com.example.konnect_backend.domain.document.entity.DocumentTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentTranslationRepository extends JpaRepository<DocumentTranslation, Long> {
    
    List<DocumentTranslation> findByDocumentId(Long documentId);
    
    Optional<DocumentTranslation> findByDocumentIdAndTranslatedLanguage(Long documentId, String translatedLanguage);
}