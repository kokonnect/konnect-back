//package com.example.konnect_backend.domain.ai.entity.log;
//
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.Setter;
//
//@Entity
//@Table(name = "llm_call_raw_data", uniqueConstraints = {@UniqueConstraint(name = "uk_llm_call_id", columnNames = "llm_call_id")})
//@Getter
//@Setter
//public class LlmCallRawData {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @OneToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "llm_call_id", nullable = false, unique = true)
//    private LlmCallMetadata metadata;
//
//    @Lob
//    @Column(columnDefinition = "MEDIUMTEXT")
//    private String prompt;
//
//    @Lob
//    @Column(columnDefinition = "MEDIUMTEXT")
//    private String response;
//}