package com.example.konnect_backend.domain.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedScheduleDto {

    private String title;

    private String memo;

    @JsonDeserialize(using = FlexibleDateTimeDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDate;

    @JsonDeserialize(using = FlexibleDateTimeDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDate;

    @Builder.Default
    private Boolean isAllDay = false;

    /**
     * LocalDate(yyyy-MM-dd)와 LocalDateTime(yyyy-MM-dd'T'HH:mm:ss) 모두 처리하는 Deserializer
     */
    public static class FlexibleDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

        private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String dateString = p.getText().trim();

            if (dateString == null || dateString.isEmpty()) {
                return null;
            }

            // LocalDateTime 형식 시도 (yyyy-MM-dd'T'HH:mm:ss)
            try {
                return LocalDateTime.parse(dateString, DATE_TIME_FORMATTER);
            } catch (DateTimeParseException e) {
                // LocalDate 형식 시도 (yyyy-MM-dd)
                try {
                    LocalDate date = LocalDate.parse(dateString, DATE_FORMATTER);
                    return date.atStartOfDay(); // 00:00:00으로 변환
                } catch (DateTimeParseException e2) {
                    // 마지막 시도: 시간만 있는 경우 처리
                    try {
                        return LocalDateTime.parse(dateString);
                    } catch (DateTimeParseException e3) {
                        throw new IOException("날짜 파싱 실패: " + dateString, e3);
                    }
                }
            }
        }
    }
}
