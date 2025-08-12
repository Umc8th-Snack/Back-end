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
/*
    /전체 기사 처리 (관리자용)
    @PostMapping("/admin/nlp/process-all")
    public ResponseEntity<Map<String, Object>> processAllArticles(
            @RequestParam(defaultValue = "false") boolean reprocess) {

        log.info("전체 기사 처리 API 호출 - 재처리: {}", reprocess);

        try {
            Map<String, Object> result = nlpService.processAllArticles(reprocess);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("전체 기사 처리 실패: {}", e.getMessage());

            Map<String, Object> errorResponse = Map.of(
                    "isSuccess", false,
                    "code", "SERVER_5001",
                    "message", "서버 내부 오류입니다.",
                    "result", null,
                    "error", e.getMessage()
            );

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // 특정 기사들 벡터화
    @PostMapping("/nlp/vectorize/articles")
    public ResponseEntity<Map<String, Object>> vectorizeArticles(
            @RequestBody List<Long> articleIds) {

        log.info("기사 벡터화 API 호출 - {}개 기사", articleIds.size());

        try {
            Map<String, Object> result = nlpService.vectorizeArticles(articleIds);

            Map<String, Object> response = Map.of(
                    "isSuccess", true,
                    "code", "SUCCESS",
                    "message", "벡터화 완료",
                    "result", result
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("벡터화 실패: {}", e.getMessage());

            Map<String, Object> errorResponse = Map.of(
                    "isSuccess", false,
                    "code", "SERVER_5001",
                    "message", "FastAPI 벡터화 서비스 호출 실패",
                    "result", null,
                    "error", e.getMessage()
            );

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // NLP 시스템 상태 확인
    @GetMapping("/nlp/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> stats = nlpService.getVectorStatistics();

            boolean isHealthy = "connected".equals(stats.get("fastapi_status"));

            Map<String, Object> response = Map.of(
                    "isSuccess", isHealthy,
                    "code", isHealthy ? "SUCCESS" : "SERVICE_UNAVAILABLE",
                    "message", isHealthy ? "NLP 서비스 정상" : "NLP 서비스 연결 실패",
                    "result", stats
            );

            return isHealthy ?
                    ResponseEntity.ok(response) :
                    ResponseEntity.status(503).body(response);

        } catch (Exception e) {
            log.error("헬스체크 실패: {}", e.getMessage());

            Map<String, Object> errorResponse = Map.of(
                    "isSuccess", false,
                    "code", "SERVER_ERROR",
                    "message", "헬스체크 실패",
                    "result", null,
                    "error", e.getMessage()
            );

            return ResponseEntity.status(503).body(errorResponse);
        }
    }

    // 통계 조회
    @GetMapping("/nlp/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> stats = nlpService.getVectorStatistics();

            Map<String, Object> response = Map.of(
                    "isSuccess", true,
                    "code", "SUCCESS",
                    "message", "통계 조회 성공",
                    "result", stats
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("통계 조회 실패: {}", e.getMessage());

            Map<String, Object> errorResponse = Map.of(
                    "isSuccess", false,
                    "code", "SERVER_ERROR",
                    "message", "통계 조회 실패",
                    "result", null,
                    "error", e.getMessage()
            );

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // 의미 기반 기사 검색 (새로 추가된 API)
    @GetMapping("/articles/search")
    public ResponseEntity<Map<String, Object>> searchArticles(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "0.3") double threshold) {

        // 1. 서비스 로직 호출
        SearchResponseDto result = nlpService.searchArticles(query, page, size, threshold);

        // 2. 성공 응답 본문을 Map으로 구성
        Map<String, Object> successResponse = Map.of(
                "isSuccess", true,
                "code", "SUCCESS",
                "message", "요청에 성공하였습니다.",
                "result", result
        );

        // 3. ResponseEntity.ok()를 사용해 200 OK 상태와 함께 응답 반환
        return ResponseEntity.ok(successResponse);
    }

    @PostMapping("/nlp/user-profile")
    public ResponseEntity<Map<String, Object>> updateUserProfile(
            @RequestBody UserProfileRequestDto requestDto) {

        nlpService.updateUserProfile(requestDto.getUserId(), requestDto.getInteractions());

        Map<String, Object> successResponse = Map.of(
                "isSuccess", true,
                "code", "SUCCESS",
                "message", "사용자 프로필 업데이트 요청 성공"
        );
        return ResponseEntity.ok(successResponse);
    }
    */
    /**
     * 특정 기사들 벡터화
     */
    @PostMapping("/vectorize/articles")
    public ResponseEntity<Map<String, Object>> vectorizeArticles(
            @RequestBody List<Long> articleIds) {

        Map<String, Object> result = nlpService.vectorizeArticles(articleIds);
        return ResponseEntity.ok(Map.of("isSuccess", true, "result", result));
    }

    /**
     * 사용자 프로필 벡터 생성/업데이트 (Spring 내부 또는 관리자용)
     */
    @PostMapping("/user-profile")
    public ResponseEntity<Map<String, Object>> updateUserProfile(
            @RequestBody UserProfileRequestDto requestDto) {

        nlpService.updateUserProfile(requestDto.getUserId(), requestDto.getInteractions());
        return ResponseEntity.ok(Map.of("isSuccess", true, "message", "사용자 프로필 업데이트 요청 성공"));
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
        Map<String, Object> result = nlpService.processAllArticles(reprocess);
        return ResponseEntity.ok(result);
    }
}