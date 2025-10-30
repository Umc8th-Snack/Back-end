package umc.snack.controller.article;

import umc.snack.domain.article.dto.ArticleDto;

import umc.snack.domain.article.dto.RelatedArticleDto;
import umc.snack.domain.article.entity.CrawledArticle;
import umc.snack.domain.term.dto.TermResponseDto;
import umc.snack.domain.user.dto.UserClicksDto;
import umc.snack.repository.article.CrawledArticleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import umc.snack.common.config.security.CustomUserDetails;
import umc.snack.common.dto.ApiResponse;
import umc.snack.service.article.ArticleService;
import umc.snack.service.user.UserClickService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(value = "/api/articles", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Article", description = "기사 관련 API")
public class ArticleController {

    private final CrawledArticleRepository crawledArticleRepository;
    private final ArticleService articleService;
    private final UserClickService userClickService;

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


    @Operation(summary = "기사 상세 정보 조회", description = "기사 요약내용, 원본 url 등 기사의 상세 정보를 제공합니다. " +
            "로그인한 사용자의 경우 자동으로 클릭 로그가 저장됩니다.")
    @GetMapping("/{articleId}")
    public ResponseEntity<ApiResponse<ArticleDto>> getArticle(
            @PathVariable Long articleId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        // 기사 조회
        ArticleDto dto = articleService.getArticleById(articleId);
        
        // 로그인한 사용자라면 클릭 로그 자동 저장 
        if (userDetails != null) {
            try {
                UserClicksDto clickDto = UserClicksDto.builder()
                    .userId(userDetails.getUserId())
                    .articleId(articleId)
                    .build();
                userClickService.saveUserClick(clickDto);
            } catch (Exception e) {
                // 클릭 로그 저장 실패해도 기사 조회는 성공 (에러 무시)
            }
        }
        
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

    @Operation(summary = "관련 기사 조회", description = "현재 보고 있는 기사와 카테고리가 같은 관련 기사를 추천합니다.")
    @GetMapping("/{articleId}/related-articles")
    public ResponseEntity<ApiResponse<List<RelatedArticleDto>>> getRelatedArticles(@PathVariable Long articleId) {
        List<RelatedArticleDto> relatedArticles = articleService.findRelatedArticles(articleId);
        if (relatedArticles.isEmpty()) {
            return ResponseEntity.ok(
                    ApiResponse.onSuccess("ARTICLE_9004", "해당 기사와 관련된 기사가 없습니다.", List.of()));
        } else {
            return ResponseEntity.ok(
                    ApiResponse.onSuccess("ARTICLE_9003", "관련 기사 조회에 성공하였습니다.", relatedArticles)
            );
        }
    }
}