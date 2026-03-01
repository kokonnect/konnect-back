package com.example.konnect_backend.domain.ai.entity;

import com.example.konnect_backend.domain.ai.type.PromptStatus;
import com.example.konnect_backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

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

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Setter
    private PromptStatus status;

    @Column(nullable = false)
    private Integer maxTokens;

    @Column
    private Long modelId;

    @OneToMany(
        mappedBy = "prompt",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @OrderBy("slotOrder ASC")
    private List<PromptSlot> slots = new ArrayList<>();

    public PromptTemplate(String moduleName, Integer version, String template, Integer maxTokens,
                          Long modelId) {
        this.moduleName = moduleName;
        this.version = version;
        this.template = template;
        this.status = PromptStatus.DRAFT;
        this.maxTokens = maxTokens;
        this.modelId = modelId;
    }

    public void addSlot(PromptSlot slot) {
        this.slots.add(slot);
        slot.setPrompt(this);
    }

    public void updateSlots(List<PromptSlot> newSlots) {
        this.slots.clear(); // 기존 관리되던 리스트를 비움
        if (newSlots != null) {
            for (PromptSlot slot : newSlots) {
                this.addSlot(slot); // 편의 메소드를 통해 주인 설정과 리스트 추가를 동시에
            }
        }
    }
}