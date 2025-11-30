package com.example.konnect_backend.domain.school.service;

import com.example.konnect_backend.domain.school.data.RegionData;
import com.example.konnect_backend.domain.school.dto.response.RegionResponse;
import com.example.konnect_backend.domain.school.dto.response.SchoolListResponse;
import com.example.konnect_backend.domain.school.dto.response.SchoolResponse;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 학교알리미 API 연동 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchoolService {

    private final RestTemplate restTemplate;

    @Value("${school.api.key}")
    private String apiKey;

    @Value("${school.api.base-url}")
    private String baseUrl;

    // 학교급 코드
    public static final String SCHOOL_KIND_ELEMENTARY = "02";  // 초등학교
    public static final String SCHOOL_KIND_MIDDLE = "03";      // 중학교
    public static final String SCHOOL_KIND_HIGH = "04";        // 고등학교

    /**
     * 시도 목록 조회
     */
    public List<RegionResponse> getSidoList() {
        return RegionData.getSidoList();
    }

    /**
     * 특정 시도의 시군구 목록 조회
     */
    public List<RegionResponse> getSggList(String sidoCode) {
        return RegionData.getSggList(sidoCode);
    }

    /**
     * 초등학교 목록 조회
     *
     * @param sidoCode 시도 코드
     * @param sggCode  시군구 코드
     * @return 초등학교 목록
     */
    public SchoolListResponse getElementarySchools(String sidoCode, String sggCode) {
        return getSchools(sidoCode, sggCode, SCHOOL_KIND_ELEMENTARY);
    }

    /**
     * 학교 목록 조회 (학교알리미 API 호출)
     *
     * @param sidoCode       시도 코드
     * @param sggCode        시군구 코드
     * @param schulKndCode   학교급 코드 (02:초등, 03:중등, 04:고등)
     * @return 학교 목록
     */
    @SuppressWarnings("unchecked")
    public SchoolListResponse getSchools(String sidoCode, String sggCode, String schulKndCode) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .queryParam("apiKey", apiKey)
                    .queryParam("apiType", "0")
                    .queryParam("sidoCode", sidoCode)
                    .queryParam("sggCode", sggCode)
                    .queryParam("schulKndCode", schulKndCode)
                    .build()
                    .toUriString();

            log.info("학교알리미 API 호출: sidoCode={}, sggCode={}, schulKndCode={}", sidoCode, sggCode, schulKndCode);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null) {
                log.error("학교알리미 API 응답이 null입니다.");
                throw new GeneralException(ErrorStatus.SCHOOL_API_FAILED);
            }

            String resultCode = (String) response.get("resultCode");
            if (!"success".equals(resultCode)) {
                String resultMsg = (String) response.get("resultMsg");
                log.error("학교알리미 API 오류: {}", resultMsg);
                throw new GeneralException(ErrorStatus.SCHOOL_API_FAILED);
            }

            List<Map<String, Object>> schoolList = (List<Map<String, Object>>) response.get("list");
            if (schoolList == null || schoolList.isEmpty()) {
                return SchoolListResponse.of(new ArrayList<>());
            }

            List<SchoolResponse> schools = schoolList.stream()
                    .map(this::mapToSchoolResponse)
                    .toList();

            log.info("학교 목록 조회 성공: {}개", schools.size());
            return SchoolListResponse.of(schools);

        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("학교알리미 API 호출 중 오류 발생", e);
            throw new GeneralException(ErrorStatus.SCHOOL_API_FAILED);
        }
    }

    /**
     * API 응답을 SchoolResponse로 변환
     */
    private SchoolResponse mapToSchoolResponse(Map<String, Object> data) {
        String schoolKindCode = getString(data, "SCHUL_KND_SC_CODE");

        return SchoolResponse.builder()
                .schoolCode(getString(data, "SCHUL_CODE"))
                .schoolName(getString(data, "SCHUL_NM"))
                .schoolKindCode(schoolKindCode)
                .schoolKindName(getSchoolKindName(schoolKindCode))
                .sidoCode(getString(data, "LCTN_SC_CODE"))
                .sidoName(getString(data, "ATPT_OFCDC_ORG_NM"))
                .sggCode(getString(data, "ADRCD_CD"))
                .sggName(getString(data, "ADRCD_NM"))
                .address(getString(data, "SCHUL_RDNDA"))
                .telNo(getString(data, "USER_TELNO"))
                .foundDate(getString(data, "FOND_YMD"))
                .foundType(getString(data, "SCHUL_FOND_TYP_CODE"))
                .build();
    }

    private String getString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    private String getSchoolKindName(String code) {
        return switch (code) {
            case "02" -> "초등학교";
            case "03" -> "중학교";
            case "04" -> "고등학교";
            case "05" -> "특수학교";
            case "06" -> "기타";
            case "07" -> "각종학교";
            default -> "";
        };
    }
}
