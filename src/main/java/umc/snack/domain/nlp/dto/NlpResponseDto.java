package umc.snack.domain.nlp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class NlpResponseDto {
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VectorizeResultDto {
        private String status;
        private int total_requested;
        private int processed;
        private List<Long> failed;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthCheckDto {
        private String fastapi_status;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessStartDto {
        private String status;
        private boolean reprocess;
    }
}
