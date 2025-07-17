package umc.snack.controller.article;

import umc.snack.domain.article.entity.CrawledArticle;
import umc.snack.repository.article.CrawledArticleRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import umc.snack.common.dto.ApiResponse;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping(value = "/api/articles", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Article", description = "기사 관련 API")
public class ArticleController {

    private final CrawledArticleRepository crawledArticleRepository;

    @Operation(summary = "기사 크롤링 상태 확인", description = "현재 크롤링 작업이 진행 중인지 확인합니다.")
    @GetMapping("/crawl/status")
    public ResponseEntity<ApiResponse<Map<String, Long>>> checkCrawlStatus() {
        long total = crawledArticleRepository.count();
        long success = crawledArticleRepository.countByStatus(CrawledArticle.Status.PROCESSED);
        long failed = crawledArticleRepository.countByStatus(CrawledArticle.Status.FAILED);

        Map<String, Long> data = new HashMap<>();
        data.put("total", total);
        data.put("success", success);
        data.put("failed", failed);
        return ResponseEntity.ok(ApiResponse.onSuccess("200", "크롤링 상태 조회 성공", data));
    }

    @Operation(summary = "기사 요약 생성", description = "AI 모델을 통해 기사 요약을 생성합니다.")
    @PostMapping("/{articleId}/summarize")
    public ResponseEntity<?> summarizeArticle(@PathVariable Long articleId) {
        // TODO: 개발 예정
        return ResponseEntity.ok("기사 요약 생성 API - 개발 예정");
    }

    @Operation(summary = "기사 단건 조회", description = "기사 ID로 기사 전체 내용을 조회합니다.")
    @GetMapping("/{articleId}")
    public ResponseEntity<?> getArticle(@PathVariable Long articleId) {
        // TODO: 개발 예정
        return ResponseEntity.ok("기사 단건 조회 API - 개발 예정");
    }

    @Operation(summary = "주요 용어 추출", description = "기사를 분석하여 주요 용어를 추출합니다.")
    @PostMapping("/{articleId}/terms/extract")
    public ResponseEntity<?> extractTerms(@PathVariable Long articleId) {
        // TODO: 개발 예정
        return ResponseEntity.ok("주요 용어 추출 API - 개발 예정");
    }

    @Operation(summary = "주요 용어 조회", description = "기사에 대해 추출된 용어들을 조회합니다.")
    @GetMapping("/{articleId}/terms")
    public ResponseEntity<?> getTerms(@PathVariable Long articleId) {
        // TODO: 개발 예정
        return ResponseEntity.ok("주요 용어 조회 API - 개발 예정");
    }
    @Operation(summary = "키워드 기반 기사 검색", description = "키워드로 기사를 검색합니다. 최신순 정렬, 페이지네이션을 지원합니다.")
    @GetMapping("/search/keyword")
    public ResponseEntity<?> searchArticlesByKeyword(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "1") int page) {
        // TODO: 개발 예정
        return ResponseEntity.ok("키워드 기반 기사 검색 API - 개발 예정");
    }

    @Operation(summary = "관련 기사 조회", description = "현재 보고 있는 기사와 카테고리가 같은 관련 기사를 추천합니다.")
    @GetMapping("/{articleId}/related-articles")
    public ResponseEntity<?> getRelatedArticles(@PathVariable Long articleId) {
        // TODO: 개발 예정
        return ResponseEntity.ok("관련 기사 조회 API - 개발 예정");
    }
}