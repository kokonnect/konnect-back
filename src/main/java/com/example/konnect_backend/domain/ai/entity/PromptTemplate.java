package com.example.konnect_backend.domain.ai.entity;

import com.example.konnect_backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "prompt_template",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_module_version",
            columnNames = {"module_name", "version"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromptTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "module_name", nullable = false, length = 100)
    private String moduleName;

    @Column(nullable = false)
    private Integer version;

    @Lob // MySQL TEXT
    @Column(nullable = false)
    private String template;

    public PromptTemplate(String moduleName, Integer version, String template) {
        this.moduleName = moduleName;
        this.version = version;
        this.template = template;
    }
}