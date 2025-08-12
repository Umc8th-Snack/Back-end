package umc.snack.controller.nlp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import umc.snack.domain.nlp.dto.SearchResponseDto;
import umc.snack.domain.nlp.dto.UserProfileRequestDto;
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
     * 사용자 프로필 벡터 생성/업데이트 (Spring 내부 또는 관리자용)
     */
    @PostMapping("/user-profile")
    public ResponseEntity<Map<String, Object>> updateUserProfile(
            @RequestBody UserProfileRequestDto requestDto) {

        try {
            nlpService.updateUserProfile(requestDto.getUserId(), requestDto.getInteractions());
            return ResponseEntity.ok(Map.of("isSuccess", true, "message", "사용자 프로필 업데이트 요청 성공"));
        } catch (Exception e) {
            log.error("사용자 프로필 업데이트 실패: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "isSuccess", false,
                    "message", "프로필 업데이트 중 오류가 발생했습니다",
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * NLP 시스템 상태 확인
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        boolean isHealthy = nlpService.checkFastApiHealth();
        return ResponseEntity.ok(Map.of("isSuccess", isHealthy, "fastapi_status", isHealthy ? "connected" : "disconnected"));
    }

    /**
     * 통계 조회
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = nlpService.getVectorStatistics();
        return ResponseEntity.ok(Map.of("isSuccess", true, "result", stats));
    }

    /**
     * 전체 기사 처리 (관리자용)
     */
    @PostMapping("/admin/process-all")
    public ResponseEntity<Map<String, Object>> processAllArticles(
            @RequestParam(defaultValue = "false") boolean reprocess) {
        try {
            Map<String, Object> result = nlpService.processAllArticles(reprocess);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("전체 기사 처리 실패: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "isSuccess", false,
                    "message", "전체 기사 처리 중 오류가 발생했습니다",
                    "error", e.getMessage()
            ));
        }
    }
}