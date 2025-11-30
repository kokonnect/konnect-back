package com.example.konnect_backend.domain.school.controller;

import com.example.konnect_backend.domain.school.dto.response.RegionResponse;
import com.example.konnect_backend.domain.school.dto.response.SchoolListResponse;
import com.example.konnect_backend.domain.school.service.SchoolService;
import com.example.konnect_backend.global.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schools")
@RequiredArgsConstructor
@Tag(name = "School", description = "학교 정보 조회 API")
public class SchoolController {

    private final SchoolService schoolService;

    @GetMapping("/regions/sido")
    @Operation(
            summary = "시도 목록 조회",
            description = "전국 시도(광역시/도) 목록을 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "성공",
                    content = @Content(
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "status": "COMMON200",
                                              "message": "OK",
                                              "data": [
                                                {"code": "11", "name": "서울특별시"},
                                                {"code": "26", "name": "부산광역시"},
                                                {"code": "41", "name": "경기도"}
                                              ]
                                            }
                                            """
                            )
                    )
            )
    })
    public ApiResponse<List<RegionResponse>> getSidoList() {
        return ApiResponse.onSuccess(schoolService.getSidoList());
    }

    @GetMapping("/regions/sgg")
    @Operation(
            summary = "시군구 목록 조회",
            description = "특정 시도의 시군구 목록을 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "성공",
                    content = @Content(
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "status": "COMMON200",
                                              "message": "OK",
                                              "data": [
                                                {"code": "11110", "name": "종로구"},
                                                {"code": "11140", "name": "중구"},
                                                {"code": "11170", "name": "용산구"}
                                              ]
                                            }
                                            """
                            )
                    )
            )
    })
    public ApiResponse<List<RegionResponse>> getSggList(
            @Parameter(description = "시도 코드", example = "11", required = true)
            @RequestParam String sidoCode
    ) {
        return ApiResponse.onSuccess(schoolService.getSggList(sidoCode));
    }

    @GetMapping("/elementary")
    @Operation(
            summary = "초등학교 목록 조회",
            description = """
                    특정 지역의 초등학교 목록을 조회합니다.

                    **사용 예시:**
                    - 서울 종로구 초등학교: `sidoCode=11&sggCode=11110`
                    - 경기 수원시 장안구: `sidoCode=41&sggCode=41111`
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "성공",
                    content = @Content(schema = @Schema(implementation = SchoolListResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503",
                    description = "학교알리미 API 호출 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ApiResponse<SchoolListResponse> getElementarySchools(
            @Parameter(description = "시도 코드", example = "11", required = true)
            @RequestParam String sidoCode,
            @Parameter(description = "시군구 코드", example = "11110", required = true)
            @RequestParam String sggCode
    ) {
        return ApiResponse.onSuccess(schoolService.getElementarySchools(sidoCode, sggCode));
    }

    @GetMapping
    @Operation(
            summary = "학교 목록 조회 (학교급 지정)",
            description = """
                    특정 지역의 학교 목록을 학교급별로 조회합니다.

                    **학교급 코드:**
                    - `02`: 초등학교
                    - `03`: 중학교
                    - `04`: 고등학교
                    - `05`: 특수학교
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "성공",
                    content = @Content(schema = @Schema(implementation = SchoolListResponse.class))
            )
    })
    public ApiResponse<SchoolListResponse> getSchools(
            @Parameter(description = "시도 코드", example = "11", required = true)
            @RequestParam String sidoCode,
            @Parameter(description = "시군구 코드", example = "11110", required = true)
            @RequestParam String sggCode,
            @Parameter(description = "학교급 코드 (02:초등, 03:중등, 04:고등)", example = "02", required = true)
            @RequestParam String schulKndCode
    ) {
        return ApiResponse.onSuccess(schoolService.getSchools(sidoCode, sggCode, schulKndCode));
    }
}
