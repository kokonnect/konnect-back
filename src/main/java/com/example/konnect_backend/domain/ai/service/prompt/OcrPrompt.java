package com.example.konnect_backend.domain.ai.service.prompt;

public abstract class OcrPrompt {
    public static final String OCR_PROMPT = """
        이 이미지에서 모든 텍스트를 추출해주세요.
        
        ## 추출 지침
        - 이미지에 보이는 모든 텍스트를 정확하게 추출
        - 원본 텍스트의 줄바꿈과 문단 구조 유지
        - 표가 있는 경우 텍스트 내용만 추출 (표 형식 유지 불필요)
        - 손글씨도 가능한 한 정확하게 인식
        - 텍스트가 없는 경우 빈 문자열 반환
        - 추출된 텍스트만 출력하고 다른 설명은 하지 마세요
        
        ## 출력 형식
        추출된 텍스트를 그대로 출력
        """;
}
