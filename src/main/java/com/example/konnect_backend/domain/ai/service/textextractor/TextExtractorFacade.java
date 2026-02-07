package com.example.konnect_backend.domain.ai.service.textextractor;

import com.example.konnect_backend.domain.ai.exception.DocumentAnalysisException;
import com.example.konnect_backend.domain.ai.exception.TextExtractionException;
import com.example.konnect_backend.domain.ai.model.vo.TextExtractionResult;
import com.example.konnect_backend.domain.ai.model.vo.UploadFile;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 사용자가 업로드한 파일로부터 텍스트를 추출합니다.
 */
@Service
@RequiredArgsConstructor
public class TextExtractorFacade {

    private final List<TextExtractor> extractors;

    public TextExtractionResult extract(UploadFile file) {
        for (TextExtractor extractor : extractors) {
            if (extractor.supports(file.fileType())) {
                TextExtractionResult result = extractor.extract(file);

                if (result.isFailed()) {
                    throw new TextExtractionException(ErrorStatus.TEXT_EXTRACTION_FAILED);
                }

                return result;
            }
        }

        throw new DocumentAnalysisException(ErrorStatus.UNSUPPORTED_FILE_TYPE);
    }
}
