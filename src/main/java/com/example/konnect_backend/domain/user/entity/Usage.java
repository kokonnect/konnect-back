package com.example.konnect_backend.domain.user.entity;

import com.example.konnect_backend.domain.user.entity.status.IdentityType;
import com.example.konnect_backend.domain.user.entity.status.UsageType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name="user_usage",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_usage_identity_date",
                columnNames = {"identityType", "identityKey", "usageType", "date"}
        )
)
public class Usage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private IdentityType identityType;

    @Enumerated(EnumType.STRING)
    private UsageType usageType;

    private String identityKey;

    private LocalDate date;

    private int count;

    public void increase() {
        this.count++;
    }
}