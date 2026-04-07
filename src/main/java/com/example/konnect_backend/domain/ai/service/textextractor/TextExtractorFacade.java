package com.example.konnect_backend.domain.ai.service.textextractor;

import com.example.konnect_backend.domain.ai.exception.DocumentAnalysisException;
import com.example.konnect_backend.domain.ai.exception.TextExtractionException;
import com.example.konnect_backend.domain.ai.domain.vo.TextExtractionResult;
import com.example.konnect_backend.domain.ai.domain.vo.UploadFile;
import com.example.konnect_backend.domain.ai.domain.vo.PipelineContext;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 사용자가 업로드한 파일로부터 텍스트를 추출합니다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TextExtractorFacade {

    private final List<TextExtractor> extractors;

    public TextExtractionResult extract(PipelineContext context) {
        UploadFile file = context.getFile();
        if (file == null) throw new IllegalStateException("Context의 file이 null일 수 없습니다.");

        for (TextExtractor extractor : extractors) {
            if (extractor.supports(file.fileType())) {
                log.debug("텍스트 추출 시작: {}", file.fileType());
                TextExtractionResult result = extractor.extract(file);

                // 각 Extractor는 서로 다른 타입을 다룬다는 가정
                if (result.isFailed()) {
                    throw new TextExtractionException(ErrorStatus.TEXT_EXTRACTION_FAILED);
                }

                log.debug("텍스트 추출 완료: {}자, 방식: {}", result.getText().length(), result.getOcrMethod());

                context.setExtractedText(result.getText());
                context.setOcrMethod(result.getOcrMethod());
                context.setPageCount(result.getPageCount());
                context.setCompletedStage(PipelineContext.PipelineStage.TEXT_EXTRACTED);
                return result;
            }
        }

        throw new DocumentAnalysisException(ErrorStatus.UNSUPPORTED_FILE_TYPE);
    }
}
