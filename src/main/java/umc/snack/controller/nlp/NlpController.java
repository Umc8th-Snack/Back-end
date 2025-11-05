package umc.snack.controller.nlp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import umc.snack.common.dto.ApiResponse;
import umc.snack.common.exception.ErrorCode;
import umc.snack.domain.nlp.dto.NlpResponseDto;
import umc.snack.service.nlp.NlpService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NlpController {

    private final NlpService nlpService;
    /**
     * 특정 기사들 벡터화
     */
    @PostMapping("/vectorize/articles")
    public ResponseEntity<Map<String, Object>> vectorizeArticles(
            @RequestBody List<Long> articleIds) {

        try {
            Map<String, Object> result = nlpService.vectorizeArticles(articleIds);
            return ResponseEntity.ok(Map.of("isSuccess", true, "result", result));
        } catch (Exception e) {
            log.error("벡터화 실패: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "isSuccess", false,
                    "message", "벡터화 처리 중 오류가 발생했습니다",
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * NLP 시스템 상태 확인
     */
    @GetMapping("/health")
    public ApiResponse<NlpResponseDto.HealthCheckDto> healthCheck() {
        NlpResponseDto.HealthCheckDto healthStatusDto = nlpService.healthCheck();

        return ApiResponse.onSuccess("NLP_9520",
                "python server가 정상적으로 연결되었습니다.",
                healthStatusDto);
    }

    /**
     * 전체 기사 처리 (admin)
     */
    @PostMapping("/admin/process-all")
    public ApiResponse<NlpResponseDto.ProcessStartDto> processAllArticles(
            @RequestParam(defaultValue = "false") boolean reprocess) {

        try {
            nlpService.processAllArticles(reprocess); // 비동기 작업 호출

            ErrorCode code = ErrorCode.FEED_9508;

            NlpResponseDto.ProcessStartDto dataDto = NlpResponseDto.ProcessStartDto.builder()
                    .status("BACKGROUND_PROCESSING_STARTED")
                    .reprocess(reprocess)
                    .build();

            return ApiResponse.onSuccess(
                    code.name(),             // "FEED_9508"
                    code.getMessage(),       // "기사 벡터화를 시작합니다."
                    dataDto                  // DTO 전달
            );

        } catch (Exception e) {
            log.error("전체 기사 처리 시작 실패: {}", e.getMessage());

            return ApiResponse.onFailure(
                    ErrorCode.SERVER_5101.name(),       // "SERVER_5101"
                    ErrorCode.SERVER_5101.getMessage(),
                    null
            );
        }
    }
}