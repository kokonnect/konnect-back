package com.example.konnect_backend.domain.document.entity;

import com.example.konnect_backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentFile extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long documentFileId;

    @ManyToOne
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    private String fileName;
    private String fileType;

    @Column(columnDefinition = "TEXT")
    private String fileUrl;
}
