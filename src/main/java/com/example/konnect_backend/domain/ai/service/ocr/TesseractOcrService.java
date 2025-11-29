package com.example.konnect_backend.domain.ai.service.ocr;

import com.example.konnect_backend.domain.ai.exception.OcrException;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
// import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;

@Slf4j
@Service("tesseractOcr")
// @Primary - Gemini Vision OCR가 기본으로 사용됨
public class TesseractOcrService implements OcrService {

    @Value("${tesseract.datapath:#{null}}")
    private String configuredDatapath;

    private String datapath;

    @PostConstruct
    public void init() {
        // 설정된 경로가 있으면 사용, 없으면 OS에 따라 기본값 설정
        if (configuredDatapath != null && !configuredDatapath.isEmpty()) {
            this.datapath = configuredDatapath;
        } else {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                this.datapath = "C:/Program Files/Tesseract-OCR/tessdata";
            } else if (os.contains("mac")) {
                this.datapath = "/usr/local/share/tessdata";
            } else {
                this.datapath = "/usr/share/tesseract-ocr/5/tessdata";
            }
        }
        log.info("Tesseract OCR 초기화 완료, datapath: {}", datapath);
    }

    @Override
    public String extractText(byte[] imageBytes, String mimeType) {
        try {
            log.info("Tesseract OCR 시작, 이미지 크기: {} bytes, MIME: {}", imageBytes.length, mimeType);

            // 이미지 바이트 -> BufferedImage 변환
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                throw new OcrException(ErrorStatus.INVALID_IMAGE_FILE);
            }

            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath(datapath);
            tesseract.setLanguage("kor+eng");

            String result = tesseract.doOCR(image);

            if (result == null || result.trim().isEmpty()) {
                log.warn("Tesseract OCR 결과 없음");
                return "";
            }

            log.info("Tesseract OCR 완료: {} 글자 추출", result.length());
            return result.trim();

        } catch (TesseractException e) {
            log.error("Tesseract OCR 처리 중 오류: {}", e.getMessage());
            throw new OcrException(ErrorStatus.OCR_FAILED);
        } catch (Exception e) {
            log.error("Tesseract 이미지 처리 실패", e);
            throw new OcrException(ErrorStatus.OCR_FAILED);
        }
    }

    @Override
    public boolean supports(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }
}
