package com.example.konnect_backend.domain.document.repository;

import com.example.konnect_backend.domain.document.entity.Document;
import com.example.konnect_backend.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    
    List<Document> findByUserOrderByCreatedAtDesc(User user);
    
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.documentFiles LEFT JOIN FETCH d.translations WHERE d.id = :id")
    Optional<Document> findByIdWithDetails(@Param("id") Long id);
    
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.documentFiles WHERE d.user = :user ORDER BY d.createdAt DESC")
    List<Document> findByUserWithFiles(@Param("user") User user);
    
    @Query("SELECT DISTINCT d FROM Document d " +
           "LEFT JOIN FETCH d.documentFiles " +
           "WHERE d.user = :user " +
           "ORDER BY d.createdAt DESC")
    List<Document> findByUserWithFilesOrderByCreatedAtDesc(@Param("user") User user, Pageable pageable);
    
    @Query("SELECT DISTINCT d FROM Document d " +
           "LEFT JOIN FETCH d.translations " +
           "WHERE d IN :documents")
    List<Document> findWithTranslationsByDocuments(@Param("documents") List<Document> documents);
}