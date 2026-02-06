package com.example.konnect_backend.domain.ai.service.extractor;

import com.example.konnect_backend.domain.ai.dto.internal.TextExtractionResult;
import com.example.konnect_backend.domain.ai.exception.TextExtractionException;
import com.example.konnect_backend.domain.ai.service.model.UploadFile;
import com.example.konnect_backend.domain.ai.service.ocr.OcrService;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfTextExtractor implements TextExtractorService {

    private static final int MIN_TEXT_LENGTH = 50;
    private static final String PDF_READER_METHOD = "PDF_READER";
    private static final String OCR_METHOD = "GEMINI_VISION_OCR";
    private static final String HYBRID_METHOD = "HYBRID";

    private final OcrService ocrService;

    @Override
    public TextExtractionResult extract(UploadFile file) {
        try {
            log.info("PDF 텍스트 추출 시작: {}", file.originalName());

            // 1단계: PagePdfDocumentReader로 텍스트 추출 시도
            TextExtractionResult pdfReaderResult = extractWithPdfReader(file);

            if (pdfReaderResult.isSuccess() &&
                pdfReaderResult.getText() != null &&
                pdfReaderResult.getText().trim().length() >= MIN_TEXT_LENGTH) {

                log.info("PDF Reader로 텍스트 추출 성공: {} 글자, {} 페이지",
                        pdfReaderResult.getText().length(),
                        pdfReaderResult.getPageCount());
                return pdfReaderResult;
            }

            // 2단계: 텍스트 부족 또는 실패 시 Gemini Vision OCR로 폴백
            log.info("PDF Reader 텍스트 부족, Gemini Vision OCR로 폴백");
            return extractWithOcr(file);

        } catch (TextExtractionException e) {
            throw e;
        } catch (Exception e) {
            log.error("PDF 텍스트 추출 중 오류", e);
            throw new TextExtractionException(ErrorStatus.PDF_PROCESSING_FAILED);
        }
    }

    private TextExtractionResult extractWithPdfReader(UploadFile file) {
        try {
            ByteArrayResource resource = new ByteArrayResource(file.inputStream().readAllBytes());

            PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
                    .withPageTopMargin(0)
                    .withPageExtractedTextFormatter(
                            ExtractedTextFormatter.builder()
                                    .withNumberOfTopTextLinesToDelete(0)
                                    .build())
                    .withPagesPerDocument(1)
                    .build();

            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource, config);
            List<Document> documents = pdfReader.read();

            if (documents.isEmpty()) {
                return TextExtractionResult.failure("PDF에서 페이지를 읽을 수 없음");
            }

            String extractedText = documents.stream()
                    .map(Document::getContent)
                    .collect(Collectors.joining("\n\n"));

            return TextExtractionResult.success(
                    extractedText != null ? extractedText.trim() : "",
                    PDF_READER_METHOD,
                    documents.size()
            );

        } catch (Exception e) {
            log.warn("PagePdfDocumentReader 실패: {}", e.getMessage());
            return TextExtractionResult.failure(e.getMessage());
        }
    }

    private TextExtractionResult extractWithOcr(UploadFile file) {
        try {
            byte[] pdfBytes = file.inputStream().readAllBytes();
            List<BufferedImage> images = convertPdfToImages(pdfBytes);

            StringBuilder combinedText = new StringBuilder();
            for (int i = 0; i < images.size(); i++) {
                byte[] imageBytes = convertImageToBytes(images.get(i));
                String pageText = ocrService.extractText(imageBytes, "image/png");
                if (pageText != null && !pageText.isEmpty()) {
                    combinedText.append(pageText).append("\n\n");
                }
                log.debug("페이지 {} OCR 완료", i + 1);
            }

            String result = combinedText.toString().trim();
            if (result.isEmpty()) {
                return TextExtractionResult.failure("PDF OCR에서 텍스트를 추출할 수 없음");
            }

            return TextExtractionResult.success(result, OCR_METHOD, images.size());

        } catch (Exception e) {
            log.error("PDF OCR 처리 실패", e);
            throw new TextExtractionException(ErrorStatus.PDF_PROCESSING_FAILED);
        }
    }

    private List<BufferedImage> convertPdfToImages(byte[] pdfBytes) throws IOException {
        try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(pdfBytes);
             PDDocument document = org.apache.pdfbox.Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();

            return java.util.stream.IntStream.range(0, pageCount)
                    .mapToObj(page -> {
                        try {
                            return renderer.renderImageWithDPI(page, 300);
                        } catch (IOException e) {
                            log.error("페이지 {} 렌더링 실패", page, e);
                            return null;
                        }
                    })
                    .filter(img -> img != null)
                    .collect(Collectors.toList());
        }
    }

    private byte[] convertImageToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }

    @Override
    public boolean supports(String mimeType) {
        return "application/pdf".equals(mimeType);
    }
}
