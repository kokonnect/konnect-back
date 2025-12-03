package com.example.konnect_backend.domain.ai.service.prompt;

import com.example.konnect_backend.domain.ai.exception.DocumentAnalysisException;
import com.example.konnect_backend.domain.ai.service.GeminiService;
import com.example.konnect_backend.domain.ai.service.pipeline.PipelineContext;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 쉬운 한국어 변환 모듈 (Gemini API 사용)
 *
 * ## 모델 선택: gemini-2.0-flash-lite
 * - 이유: 단순한 텍스트 변환 작업
 * - 어려운 한국어 → 쉬운 한국어 재작성
 * - RPD: 1,000회/일로 여유로움
 * - 빠른 응답 속도, 비용 효율적
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KoreanSimplifierModule implements PromptModule<String, String> {

    private final GeminiService geminiService;

    @Getter
    private String lastRawResponse;
    @Getter
    private long lastProcessingTimeMs;

    // Lite 모델 사용 (단순 텍스트 변환)
    public static final String MODEL_NAME = "gemini-2.0-flash-lite";
    public static final double TEMPERATURE = 0.3;
    public static final int MAX_TOKENS = 4000;
    public static final String PROMPT_TEMPLATE_NAME = "KOREAN_SIMPLIFIER_PROMPT_V1";

    private static final String SIMPLIFY_PROMPT_TEMPLATE = """
            다음 한국어 학교 가정통신문을 외국인도 쉽게 이해할 수 있는 쉬운 한국어로 다시 작성해주세요.

            ## 쉬운 한국어 작성 지침
            - 한자어나 어려운 단어를 쉬운 말로 바꾸기
              예: "여가선용" → "남는 시간을 잘 쓰기"
              예: "건전하지 못한" → "좋지 않은"
              예: "각별히" → "특별히", "더 많이"
            - 긴 문장은 짧게 나누기
            - 존댓말은 유지하되 자연스럽게
            - 날짜, 시간, 장소 등 중요 정보는 그대로 유지
            - 문서의 전체 내용과 구조는 유지
            - 의미가 달라지지 않도록 주의

            ## 출력 형식 규칙 (필수)
            - 마크다운 문법 사용 금지 (###, **, *, -, |, 표 등 사용하지 않기)
            - 순수 텍스트로만 작성
            - 줄바꿈은 허용하되, 특수 기호나 서식 없이 일반 문장으로 작성

            ## 원본 텍스트
            %s

            ## 쉬운 한국어 버전:
            """;

    @Override
    public String process(String text, PipelineContext context) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("쉬운 한국어 재작성 시작 (Gemini Lite 모델)");

            String promptContent = String.format(SIMPLIFY_PROMPT_TEMPLATE, text);

            // Gemini Lite 모델 사용 (preferPrimary = false)
            String simplifiedText = geminiService.generateSimpleContent(promptContent, TEMPERATURE, MAX_TOKENS);

            this.lastRawResponse = simplifiedText;
            this.lastProcessingTimeMs = System.currentTimeMillis() - startTime;

            if (simplifiedText == null || simplifiedText.trim().isEmpty()) {
                throw new DocumentAnalysisException(ErrorStatus.DOCUMENT_ANALYSIS_FAILED);
            }

            context.addLog("쉬운 한국어 재작성 완료: " + simplifiedText.length() + "자");

            return simplifiedText.trim();

        } catch (DocumentAnalysisException e) {
            this.lastProcessingTimeMs = System.currentTimeMillis() - startTime;
            throw e;
        } catch (Exception e) {
            this.lastProcessingTimeMs = System.currentTimeMillis() - startTime;
            log.error("쉬운 한국어 재작성 실패", e);
            throw new DocumentAnalysisException(ErrorStatus.DOCUMENT_ANALYSIS_FAILED);
        }
    }

    @Override
    public String getModuleName() {
        return "KoreanSimplifier";
    }
}
