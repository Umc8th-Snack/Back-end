package umc.snack.service.feed;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;
import umc.snack.converter.feed.FeedConverter;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.feed.dto.ArticleInFeedDto;
import umc.snack.domain.nlp.dto.FeedResponseDto;
import umc.snack.domain.nlp.dto.RecommendedArticleDto;
import umc.snack.domain.nlp.dto.SearchResponseDto;
import umc.snack.domain.nlp.dto.UserInteractionDto;
import umc.snack.domain.user.dto.UserCategoryScoreDto;
import umc.snack.domain.user.entity.UserClicks;
import umc.snack.domain.user.entity.UserScrap;
import umc.snack.repository.article.ArticleRepository;
import umc.snack.repository.feed.CategoryRepository;
import umc.snack.repository.feed.FeedRepository;
import umc.snack.repository.feed.UserClickRepository;
import umc.snack.repository.scrap.UserScrapRepository;
import umc.snack.service.nlp.NlpService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedServiceImpl implements FeedService{
    private final FeedRepository feedRepository;
    private final CategoryRepository categoryRepository;
    private final FeedConverter feedConverter;
    private final NlpService nlpService;

    private final UserScrapRepository userScrapRepository;
    private final UserClickRepository userClickRepository;
    private final ArticleRepository articleRepository;

    private final UserPreferenceService userPreferenceService;

    private static final int PAGE_SIZE = 16;

    @Override
    public ArticleInFeedDto getMainFeedByCategories(List<String> categoryNames, Long lastArticleId, Long userId) {
        // 카테고리 다중 선택
        // 유효하지 않은 커서 값에 대한 예외처리
        if (lastArticleId != null && lastArticleId <= 0) {
            throw new CustomException(ErrorCode.FEED_9603);
        }

        // 유효하지 않은 카테고리에 대한 예외처리
        for (String categoryName : categoryNames) {
            if (!categoryRepository.existsByCategoryName(categoryName)) {
                throw new CustomException(ErrorCode.FEED_9601);
            }
        }

        Pageable pageable = PageRequest.of(0, PAGE_SIZE, Sort.by("publishedAt").descending()
                .and(Sort.by("articleId").descending()));

        Slice<Article> articleSlice;

        if (lastArticleId == null) {
            articleSlice = feedRepository.findByCategoryName(categoryNames, pageable);
        } else {
            articleSlice = feedRepository.findByCategoryNameWithCursor(categoryNames, lastArticleId, pageable);
        }

        String responseCategoryName = String.join(",", categoryNames);
        return buildFeedResponse(responseCategoryName, articleSlice);



    }

    @Override
    @Transactional(readOnly = true) // readOnly 추가
    public ArticleInFeedDto getPersonalizedFeed(Long userId, Long lastArticleId) {
        // 1. UserPreferenceService를 사용해 사용자의 상위 3개 관심 카테고리 DTO 조회
        List<UserCategoryScoreDto> topCategoriesScores = userPreferenceService.calculateCategoryScores(userId);

        if (topCategoriesScores.isEmpty()) {
            return ArticleInFeedDto.builder()
                    .category("맞춤 피드")
                    .hasNext(false)
                    .nextCursorId(null)
                    .articles(new ArrayList<>())
                    .build();
        }

        // **** 1. DTO에서 Category ID 리스트를 추출 ****
        List<Long> topCategoryIds = topCategoriesScores.stream()
                .map(UserCategoryScoreDto::getCategoryId) // .getCategory().getName() 대신 .getCategoryId() 사용
                .collect(Collectors.toList());

        // 2. 메인 피드 로직을 재사용하여 상위 카테고리의 기사 조회
        if (lastArticleId != null && lastArticleId <= 0) {
            throw new CustomException(ErrorCode.FEED_9603); // 유효하지 않은 커서
        }

        Pageable pageable = PageRequest.of(0, PAGE_SIZE, Sort.by("publishedAt").descending()
                .and(Sort.by("id").descending()));

        Slice<Article> articleSlice;

        // **** 2. 새로 만든 Repository 메소드 호출 ****
        if (lastArticleId == null) {
            // 커서가 없으면 카테고리 ID로 최신 기사 조회
            articleSlice = feedRepository.findByCategoryId(topCategoryIds, pageable);
        } else {
            // 커서가 있으면 해당 커서 다음부터 기사 조회
            articleSlice = feedRepository.findByCategoryIdWithCursor(topCategoryIds, lastArticleId, pageable);
        }

        if (!articleSlice.hasContent()) {
            return null; // 기사가 더이상 없으면 빈 결과 반환
        }

        // 3. 응답 DTO 생성
        return buildFeedResponse("맞춤 피드", articleSlice);
    }

    @Override
    public SearchResponseDto searchArticlesByQuery(String query, int page, int size, double threshold) {
        return nlpService.searchArticles(query, page, size, threshold);
    }

    // **** 2. 누락되었던 private 헬퍼 메소드 ****
    private ArticleInFeedDto buildFeedResponse(String categoryName, Slice<Article> articleSlice) {
        if (!articleSlice.hasContent()) {
            throw new CustomException(ErrorCode.FEED_9502);
        }
        List<Article> articles = articleSlice.getContent();
        Long nextCursorId = articleSlice.hasNext() ? articles.get(articles.size() - 1).getArticleId() : null;
        return feedConverter.toArticleInFeedDto(categoryName, articleSlice.hasNext(), nextCursorId, articles);
    }

}