package com.example.konnect_backend.domain.ai.dto.internal;

import com.example.konnect_backend.domain.ai.dto.response.ExtractedScheduleDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionResult {

    @Builder.Default
    private List<ExtractedScheduleDto> schedules = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> additionalInfo = new HashMap<>();

    public static ExtractionResult empty() {
        return ExtractionResult.builder()
                .schedules(new ArrayList<>())
                .additionalInfo(new HashMap<>())
                .build();
    }
}
