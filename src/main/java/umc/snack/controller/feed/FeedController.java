package umc.snack.controller.feed;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import umc.snack.common.config.security.CustomUserDetails;
import umc.snack.common.dto.ApiResponse;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.feed.dto.ArticleInFeedDto;
import umc.snack.domain.nlp.dto.SearchResponseDto;
import umc.snack.repository.user.SearchKeywordRepository;
import umc.snack.service.feed.FeedService;
import umc.snack.service.nlp.NlpService;
import umc.snack.service.user.SearchKeywordService;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Feed", description = "피드 관련 API")
public class FeedController {
    private final FeedService feedService;
    private final NlpService nlpService;
    private final SearchKeywordService searchKeywordService;
    @Operation(summary = "메인 피드에서 기사 제공", description = "메인 피드에서 특정 카테고리의 기사를 무한스크롤 조회합니다.")
    @GetMapping("/feeds/main")
    public ApiResponse<ArticleInFeedDto> getMainFeedArticles(
            @RequestParam(value = "category", required = false) List<String> category,
            @RequestParam(required = false) Long lastArticleId,
            @AuthenticationPrincipal Long userId) {

        ArticleInFeedDto responseDto = feedService.getMainFeedByCategories(category, lastArticleId, userId);

        return ApiResponse.onSuccess("FEED_9501", "메인 피드 조회에 성공하였습니다", responseDto);
    }


    @Operation(summary = "의미 기반 기사 검색", description = "검색어를 기반으로 의미적으로 유사한 기사를 조회합니다.\n\n" +
            "※ 로그인한 경우에만 검색어가 저장됩니다.")
    @GetMapping("articles/search")
    public ApiResponse<SearchResponseDto> searchArticles(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            @RequestParam(value = "threshold", defaultValue = "0.7") double threshold,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // 로그인한 경우에만 저장
        if (userDetails != null) {
            Long userId = userDetails.getUser().getUserId();
            searchKeywordService.saveKeyword(userId, query);
        }

        SearchResponseDto result = feedService.searchArticlesByQuery(query, page, size, threshold);

        return ApiResponse.onSuccess("FEED_9702", "의미 기반 검색에 성공하였습니다", result);
    }

    @Operation(summary = "맞춤 피드에서 기사 제공", description = "사용자의 상위 관심 카테고리 3개에 대한 기사를 최신순으로 무한스크롤 조회합니다.")
    @Parameter(name = "lastArticleId", description = "마지막으로 조회한 기사의 ID. 첫번째 조회시에는 생략")
    @GetMapping("feeds/personalized")
    public ApiResponse<ArticleInFeedDto> getPersonalizedFeedArticles(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Long lastArticleId) {

        if (userDetails == null) {
            return ApiResponse.onFailure("FEED_9604", "로그인이 필요한 서비스입니다.", null);
        }

        Long userId = userDetails.getUserId();

        ArticleInFeedDto responseDto = feedService.getPersonalizedFeed(userId, lastArticleId);
        if (responseDto.getArticles().isEmpty()) {
            return ApiResponse.onSuccess("FEED_9504", "맞춤 피드의 기사를 찾을 수 없습니다.", null);
        }

        return ApiResponse.onSuccess("FEED_9503", "맞춤 피드 조회에 성공하였습니다", responseDto);
    }
}