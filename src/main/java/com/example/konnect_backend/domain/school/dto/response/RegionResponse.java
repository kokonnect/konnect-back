package com.example.konnect_backend.domain.school.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "지역(시도/시군구) 정보 응답 DTO")
public class RegionResponse {

    @Schema(description = "코드", example = "11")
    private String code;

    @Schema(description = "명칭", example = "서울특별시")
    private String name;

    public static RegionResponse of(String code, String name) {
        return RegionResponse.builder()
                .code(code)
                .name(name)
                .build();
    }
}
