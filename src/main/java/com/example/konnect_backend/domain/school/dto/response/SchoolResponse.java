package com.example.konnect_backend.domain.school.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "학교 정보 응답 DTO")
public class SchoolResponse {

    @Schema(description = "학교 코드", example = "S000003563")
    @JsonProperty("schoolCode")
    private String schoolCode;

    @Schema(description = "학교명", example = "서울사대부설초등학교")
    @JsonProperty("schoolName")
    private String schoolName;

    @Schema(description = "학교급 코드 (02:초등, 03:중등, 04:고등)", example = "02")
    @JsonProperty("schoolKindCode")
    private String schoolKindCode;

    @Schema(description = "학교급 명칭", example = "초등학교")
    @JsonProperty("schoolKindName")
    private String schoolKindName;

    @Schema(description = "시도 코드", example = "11")
    @JsonProperty("sidoCode")
    private String sidoCode;

    @Schema(description = "시도명", example = "서울특별시")
    @JsonProperty("sidoName")
    private String sidoName;

    @Schema(description = "시군구 코드", example = "11110")
    @JsonProperty("sggCode")
    private String sggCode;

    @Schema(description = "시군구명", example = "종로구")
    @JsonProperty("sggName")
    private String sggName;

    @Schema(description = "도로명 주소")
    @JsonProperty("address")
    private String address;

    @Schema(description = "전화번호", example = "02-768-1500")
    @JsonProperty("telNo")
    private String telNo;

    @Schema(description = "설립일", example = "18950416")
    @JsonProperty("foundDate")
    private String foundDate;

    @Schema(description = "설립 유형 (공립, 사립 등)", example = "부설")
    @JsonProperty("foundType")
    private String foundType;
}
