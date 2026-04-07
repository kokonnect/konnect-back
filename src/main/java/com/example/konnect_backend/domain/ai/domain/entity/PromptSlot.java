package com.example.konnect_backend.domain.ai.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * 각 프롬프트를 실행하기 위해 필요한 변수들을 저장한다. <br />
 * 모두 필수값임을 가정하고, 값을 제공하지 않으면 잘못되었다고 가정한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "prompt_slot",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_prompt_slot_order",
            columnNames = {"prompt_id", "slot_order"}
        ), @UniqueConstraint(
            name = "uk_prompt_slot_key",
            columnNames = {"prompt_id", "slot_key"}
        )
    }
)
public class PromptSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String slotKey; // 변수명
    private Integer slotOrder; // UI에서 표시할 순서

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_id", nullable = false)
    @Setter
    private PromptTemplate prompt;

    public PromptSlot(String slotKey, Integer slotOrder, PromptTemplate prompt) {
        this.slotKey = slotKey;
        this.slotOrder = slotOrder;
        this.prompt = prompt;
    }

    public PromptSlot withPromptTemplate(PromptTemplate template) {
        return new PromptSlot(this.slotKey, this.slotOrder, template);
    }
}