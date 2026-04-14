package com.example.konnect_backend.domain.ai.service.pipeline;

import com.example.konnect_backend.domain.ai.domain.vo.ExtractedText;
import com.example.konnect_backend.domain.ai.domain.vo.PipelineContext;
import com.example.konnect_backend.domain.ai.domain.vo.UploadFile;
import com.example.konnect_backend.domain.ai.dto.internal.ExtractionResult;
import com.example.konnect_backend.domain.ai.dto.response.DifficultExpressionDto;
import com.example.konnect_backend.domain.ai.dto.response.DocumentAnalysisResponse;
import com.example.konnect_backend.domain.ai.service.history.AnalysisHistoryService;
import com.example.konnect_backend.domain.ai.service.log.AnalysisLogService;
import com.example.konnect_backend.domain.ai.type.TargetLanguage;
import com.example.konnect_backend.domain.user.entity.Device;
import com.example.konnect_backend.domain.user.entity.User;
import com.example.konnect_backend.domain.user.entity.status.UsageType;
import com.example.konnect_backend.domain.user.repository.DeviceRepository;
import com.example.konnect_backend.domain.user.repository.UserRepository;
import com.example.konnect_backend.domain.user.service.UsageFacade;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.example.konnect_backend.domain.ai.interceptor.AnalysisInterceptor.REQUEST_ID_KEY;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentAnalysisPipeline {

    private final AnalysisHistoryService analysisHistoryService;
    private final AnalysisLogService analysisLogService;
    private final PipelineExecutor pipelineExecutor;

    private final UserRepository userRepository;
    private final UsageFacade usageFacade;
    private final DeviceRepository deviceRepository;

    @Transactional
    public DocumentAnalysisResponse analyze(UploadFile file, Long requesterId, String deviceUuid) {

        // 사용량 증가
        usageFacade.validateAndIncrease(UsageType.DOCUMENT, deviceUuid);

        UUID requestId = UUID.fromString(MDC.get(REQUEST_ID_KEY));
        log.debug("[analyze] requestId: {}", requestId);
        User user = getUser(requesterId);
        TargetLanguage targetLanguage = getTargetLanguage(user, deviceUuid);

        PipelineContext context = PipelineContext.builder()
                .requestId(requestId)
                .targetLanguage(targetLanguage)
                .completedStage(PipelineContext.PipelineStage.NONE)
                .file(file)
                .processingLogs(new ArrayList<>())
                .build();

        return executePipeline(requestId, file, user, deviceUuid, context);
    }

    private DocumentAnalysisResponse executePipeline(UUID requestId, UploadFile file, User user,
                                                     String deviceUuid, PipelineContext context) {
        long startTime = System.currentTimeMillis();

        try {
            log.debug("문서 분석 파이프라인 시작: requestId={}, 파일={}, 타입={}, 언어={}", requestId,
                file.originalName(), file.fileType(), context.getTargetLanguage().getDisplayName());

            pipelineExecutor.execute(context);

            LocalDateTime now = LocalDateTime.now();
            long processingTime = System.currentTimeMillis() - startTime;

            Long requestLogId = analysisLogService.succeed(context, processingTime, now, user == null ? null : user.getId());
            Long analysisId = analysisHistoryService.saveHistory(
                    user == null ? null : user.getId(),
                    deviceUuid,
                    file,
                    context.getTargetLanguage(),
                    requestLogId,
                    new ExtractedText(context.getExtractedText()),
                    context.getTranslatedText(),
                    context.getSummary(),
                    now
            );

            return buildSuccessResponse(analysisId, file, context);
        } catch (Exception e) {
            log.error("문서 분석 파이프라인 실패: requestId={}", requestId, e);

            LocalDateTime now = LocalDateTime.now();

            // 실패 요청 기록 저장
            long processingTime = System.currentTimeMillis() - startTime;

            analysisLogService.fail(context, processingTime, now, user == null ? null : user.getId());

            throw e;
        }
    }

    private User getUser(Long userId) {
        // 미인증 사용자 (게스트용)
        if (userId == null) {
            return null;
        }
        // UserService가 현재 인증 정보를 직접 꺼내써서 userId로 직접 접근하는 게 의미 상 명확
        return userRepository.findById(userId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
    }

    private TargetLanguage getTargetLanguage(User user, String deviceUuid) {
        // 로그인 사용자 우선
        if (user != null && user.getLanguage() != null) {
            return TargetLanguage.fromLanguage(user.getLanguage());
        }

        // 게스트 → device language
        if (deviceUuid != null) {
            return deviceRepository.findById(deviceUuid)
                    .map(Device::getLanguage)
                    .filter(lang -> lang != null)
                    .map(TargetLanguage::fromLanguage)
                    .orElse(TargetLanguage.KOREAN);
        }

        // fallback
        return TargetLanguage.KOREAN;
    }

    private DocumentAnalysisResponse buildSuccessResponse(Long analysisId, UploadFile file,
                                                          PipelineContext context) {
        String extractedText = context.getExtractedText();
        ExtractionResult extraction = context.getExtractionResult();
        List<DifficultExpressionDto> difficultExpressions = context.getDifficultExpressions();
        String translatedText = context.getTranslatedText();
        String summary = context.getSummary();

        return DocumentAnalysisResponse.builder().analysisId(analysisId)
            .extractedText(extractedText).difficultExpressions(difficultExpressions)
            .translatedText(translatedText).summary(summary)
            .extractedSchedules(extraction.getSchedules()).originalFileName(file.originalName())
            .build();
    }
}
