package umc.snack.controller.feed;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import umc.snack.common.dto.ApiResponse;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.feed.dto.ArticleInFeedDto;
import umc.snack.service.feed.FeedService;

import java.util.List;

@RestController
@RequestMapping("/api/feeds")
@RequiredArgsConstructor
@Tag(name = "Feed", description = "피드 관련 API")
public class FeedController {
    private final FeedService feedService;
/*
    카테고리 단일 선택
    @Operation(summary = "메인 피드에서 기사 제공", description = "메인 피드에서 특정 카테고리의 기사를 무한스크롤 조회합니다.")
    @Parameters({
            @Parameter(name = "category", description = "조회할 카테고리 이름 (예: IT/과학)", required = true),
            @Parameter(name = "lastArticleId", description = "마지막으로 조회한 기사의 ID. 첫번째 조회시에는 생략")
    })
    @GetMapping("/main")
    public ApiResponse<ArticleInFeedDto> getMainFeedArticles(
            @RequestParam String category,
            @RequestParam(required = false) Long lastArticleId,
            @AuthenticationPrincipal Long userId) {
        ArticleInFeedDto responseDto = feedService.getMainFeedByCategory(category, lastArticleId, userId);
        return ApiResponse.onSuccess("FEED_9501", "메인 피드 조회에 성공하였습니다", responseDto);
    }
 */
    // 카테고리 다중 선택
    @Operation(summary = "메인 피드에서 기사 제공", description = "메인 피드에서 특정 카테고리의 기사를 무한스크롤 조회합니다.")
    @Parameters({
            @Parameter(name = "category", description = "조회할 카테고리 이름들 (예: category=IT/과학&category=정치)", required = true),
            @Parameter(name = "lastArticleId", description = "마지막으로 조회한 기사의 ID. 첫번째 조회시에는 생략")
    })
    @GetMapping("/main")
    public ApiResponse<ArticleInFeedDto> getMainFeedArticles(
            @RequestParam List<String> category,
            @RequestParam(required = false) Long lastArticleId,
            @AuthenticationPrincipal Long userId) {
        ArticleInFeedDto responseDto = feedService.getMainFeedByCategories(category, lastArticleId, userId);
        return ApiResponse.onSuccess("FEED_9501", "메인 피드 조회에 성공하였습니다", responseDto);
    }


    @Operation(summary = "맞춤 피드에서 기사 제공", description = "사용자에게 맞춤화된 기사를 무한스크롤 조회합니다.")
    @GetMapping("/personalized")
    public ResponseEntity<?> getPersonalizedFeedArticles(
            @RequestParam(defaultValue = "1") int page) {
        // TODO: 개발 예정
        return ResponseEntity.ok("맞춤 피드 기사 제공 API - 개발 예정 (page: " + page + ")");
    }
}