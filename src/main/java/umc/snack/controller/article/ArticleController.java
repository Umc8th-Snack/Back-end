package umc.snack.controller.article;

import umc.snack.domain.article.dto.ArticleDto;

import umc.snack.domain.article.entity.CrawledArticle;
import umc.snack.domain.term.dto.TermResponseDto;
import umc.snack.repository.article.CrawledArticleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import umc.snack.common.dto.ApiResponse;
import umc.snack.service.article.ArticleService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/articles", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Article", description = "기사 관련 API")
public class ArticleController {

    private final CrawledArticleRepository crawledArticleRepository;
    private final ArticleService articleService;

    @Operation(summary = "기사 크롤링 상태 확인", description = "현재 크롤링 작업이 진행 중인지 확인합니다.")
    @GetMapping("/crawl/status")
    public ResponseEntity<ApiResponse<Map<String, Long>>> checkCrawlStatus() {
        long total   = crawledArticleRepository.count();
        long success = crawledArticleRepository.countByStatus(CrawledArticle.Status.PROCESSED);
        long failed  = crawledArticleRepository.countByStatus(CrawledArticle.Status.FAILED);

        Map<String, Long> result = Map.of(
                "total",   total,
                "success", success,
                "failed",  failed
        );

        return ResponseEntity.ok(
                ApiResponse.onSuccess(
                        "ARTICLE_8001",
                        "크롤링 상태 조회 성공",
                        result
                )
        );
    }


    @Operation(summary = "기사 상세 정보 조회", description = "기사 요약내용, 원본 url 등 기사의 상세 정보를 제공합니다.")
    @GetMapping("/{articleId}")
    public ResponseEntity<ApiResponse<ArticleDto>> getArticle(@PathVariable Long articleId) {
        ArticleDto dto = articleService.getArticleById(articleId);
        return ResponseEntity.ok(
                ApiResponse.onSuccess(
                        "ARTICLE_9001",
                        "기사 정보를 성공적으로 불러왔습니다.",
                        dto
                )
        );
    }


    @Operation(summary = "주요 용어 조회", description = "기사에 대해 추출된 용어들을 조회합니다.")
    @GetMapping("/{articleId}/terms")
    public ResponseEntity<ApiResponse<List<TermResponseDto>>> getTerms(@PathVariable Long articleId) {
        List<TermResponseDto> terms = articleService.getTermsByArticleId(articleId);
        return ResponseEntity.ok(
                ApiResponse.onSuccess(
                        "ARTICLE_9002",
                        "기사 주요 용어 조회 성공",
                        terms
                )
        );
    }
/*
    @Operation(summary = "키워드 기반 기사 검색", description = "키워드로 기사를 검색합니다. 최신순 정렬, 페이지네이션을 지원합니다.")
    @GetMapping("/search/keyword")
    public ResponseEntity<?> searchArticlesByKeyword(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "1") int page) {
        // TODO: 개발 예정
        return ResponseEntity.ok("키워드 기반 기사 검색 API - 개발 예정");
    }
*/
    @Operation(summary = "관련 기사 조회", description = "현재 보고 있는 기사와 카테고리가 같은 관련 기사를 추천합니다.")
    @GetMapping("/{articleId}/related-articles")
    public ResponseEntity<?> getRelatedArticles(@PathVariable Long articleId) {
        // TODO: 개발 예정
        return ResponseEntity.ok("관련 기사 조회 API - 개발 예정");
    }
}