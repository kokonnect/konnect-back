package com.example.konnect_backend.domain.school.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "학교 목록 응답 DTO")
public class SchoolListResponse {

    @Schema(description = "총 학교 수")
    private int totalCount;

    @Schema(description = "학교 목록")
    private List<SchoolResponse> schools;

    public static SchoolListResponse of(List<SchoolResponse> schools) {
        return SchoolListResponse.builder()
                .totalCount(schools.size())
                .schools(schools)
                .build();
    }
}
