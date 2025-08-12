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
import umc.snack.domain.nlp.dto.SearchResponseDto;
import umc.snack.service.feed.FeedService;
import umc.snack.service.nlp.NlpService;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Feed", description = "피드 관련 API")
public class FeedController {
    private final FeedService feedService;
    private final NlpService nlpService;
    // 카테고리 다중 선택
    @Operation(summary = "메인 피드에서 기사 제공", description = "메인 피드에서 특정 카테고리의 기사를 무한스크롤 조회합니다.")
    @Parameters({
            @Parameter(name = "category", description = "조회할 카테고리 이름들 (예: category=IT/과학&category=정치)", required = true),
            @Parameter(name = "lastArticleId", description = "마지막으로 조회한 기사의 ID. 첫번째 조회시에는 생략")
    })
    @GetMapping("/feeds/main")
    public ApiResponse<ArticleInFeedDto> getMainFeedArticles(
            @RequestParam List<String> category,
            @RequestParam(required = false) Long lastArticleId,
            @AuthenticationPrincipal Long userId) {
        ArticleInFeedDto responseDto = feedService.getMainFeedByCategories(category, lastArticleId, userId);
        return ApiResponse.onSuccess("FEED_9501", "메인 피드 조회에 성공하였습니다", responseDto);
    }


    @Operation(summary = "의미 기반 기사 검색", description = "검색어를 기반으로 의미적으로 유사한 기사를 조회합니다.")
    @GetMapping("articles/search")
    public ApiResponse<SearchResponseDto> searchArticles(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "0.7") double threshold) {

        SearchResponseDto result = feedService.searchArticlesByQuery(query, page, size, threshold);

        if (result.getArticles().isEmpty()) {
            return ApiResponse.onSuccess("FEED_9808", "검색결과가 없습니다.", null);
        }
        return ApiResponse.onSuccess("FEED_9702", "의미 기반 검색에 성공하였습니다", result);
    }

    @Operation(summary = "맞춤 피드에서 기사 제공", description = "사용자의 상위 관심 카테고리 3개에 대한 기사를 최신순으로 무한스크롤 조회합니다.")

    @Parameters({
            @Parameter(name = "lastArticleId", description = "마지막으로 조회한 기사의 ID. 첫번째 조회시에는 생략")
    })
    @GetMapping("feeds/personalized")
    public ApiResponse<ArticleInFeedDto> getPersonalizedFeedArticles(
            @RequestParam(required = false) Long lastArticleId,
            @AuthenticationPrincipal Long userId) {

        ArticleInFeedDto responseDto = feedService.getPersonalizedFeed(userId, lastArticleId);
        if (responseDto.getArticles().isEmpty()) {
            return ApiResponse.onSuccess("FEED_9504", "맞춤 피드의 기사를 찾을 수 없습니다.", null);
        }

        return ApiResponse.onSuccess("FEED_9503", "맞춤 피드 조회에 성공하였습니다", responseDto);
    }
}