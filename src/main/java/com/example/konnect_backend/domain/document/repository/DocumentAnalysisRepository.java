package com.example.konnect_backend.domain.document.repository;

import com.example.konnect_backend.domain.document.entity.Document;
import com.example.konnect_backend.domain.document.entity.DocumentAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentAnalysisRepository extends JpaRepository<DocumentAnalysis, Long> {

    Optional<DocumentAnalysis> findByDocument(Document document);

    Optional<DocumentAnalysis> findByDocumentId(Long documentId);
}
