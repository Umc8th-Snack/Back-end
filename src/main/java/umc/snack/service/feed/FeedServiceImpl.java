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
import umc.snack.domain.nlp.dto.*;
import umc.snack.domain.user.entity.SearchKeyword;
import umc.snack.domain.user.entity.UserClicks;
import umc.snack.domain.user.entity.UserScrap;
import umc.snack.repository.article.ArticleRepository;
import umc.snack.repository.feed.CategoryRepository;
import umc.snack.repository.feed.FeedRepository;
import umc.snack.repository.user.UserClickRepository;
import umc.snack.repository.scrap.UserScrapRepository;
import umc.snack.repository.user.SearchKeywordRepository;
import umc.snack.service.nlp.NlpService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class FeedServiceImpl implements FeedService {
    private final FeedRepository feedRepository;
    private final CategoryRepository categoryRepository;
    private final FeedConverter feedConverter;
    private final NlpService nlpService;

    private final UserScrapRepository userScrapRepository;
    private final UserClickRepository userClickRepository;
    private final SearchKeywordRepository searchKeywordRepository;
    private final ArticleRepository articleRepository;

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
    @Transactional(readOnly = true)
    public ArticleInFeedDto getPersonalizedFeed(Long userId, Long lastArticleId) {

        if (lastArticleId != null && lastArticleId <= 0) {
            throw new CustomException(ErrorCode.FEED_9603);
        }

        // 1. 사용자의 최근 행동로그 조회
        List<UserScrap> scraps = userScrapRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId);
        List<UserClicks> clicks = userClickRepository.findTop15ByUserIdOrderByCreatedAtDesc(userId);
        List<SearchKeyword> searches = searchKeywordRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);

        List<UserInteractionDto> interactions = new ArrayList<>();
        scraps.forEach(scrap -> interactions.add(new UserInteractionDto(scrap.getArticle().getArticleId(), "scrap")));
        clicks.forEach(click -> interactions.add(new UserInteractionDto(click.getArticle().getArticleId(), "click")));
        searches.forEach(search -> interactions.add(new UserInteractionDto("search", search.getKeyword())));

        if (!interactions.isEmpty()) {
            nlpService.updateUserProfile(userId, interactions);
        }

        // FastAPI한테 맞춤피드 페이지 요청
        FeedResponseDto recommendedFeed = nlpService.getPersonalizedFeed(userId, 0, 48);

        List<Long> articleIds = recommendedFeed.getArticles().stream()
                .map(RecommendedArticleDto::getArticleId)
                .collect(Collectors.toList());

        List<Long> filteredIds;
        if (lastArticleId == null) {
            // 첫 페이지: 처음 PAGE_SIZE개만 가져오기
            filteredIds = articleIds.stream()
                    .limit(PAGE_SIZE)
                    .collect(Collectors.toList());
        } else {
            // 다음 페이지: lastArticleId 이후의 기사들 중 PAGE_SIZE개 가져오기
            int lastIndex = articleIds.indexOf(lastArticleId);
            if (lastIndex == -1 || lastIndex >= articleIds.size() - 1) {
                // 더 이상 가져올 데이터가 없음
                return feedConverter.toArticleInFeedDto("맞춤 피드", false, null, new ArrayList<>());
            }

            filteredIds = articleIds.stream()
                    .skip(lastIndex + 1)
                    .limit(PAGE_SIZE)
                    .collect(Collectors.toList());
        }

        if (filteredIds.isEmpty()) {
            return feedConverter.toArticleInFeedDto("맞춤 피드", false, null, new ArrayList<>());
        }

        Map<Long, Article> articlesMap = articleRepository.findAllById(filteredIds).stream()
                .collect(Collectors.toMap(Article::getArticleId, article -> article));

        List<Article> sortedArticles = filteredIds.stream()
                .map(articlesMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (sortedArticles.isEmpty()) {
            return feedConverter.toArticleInFeedDto("맞춤 피드", false, null, new ArrayList<>());
        }

        boolean hasNext;
        if (lastArticleId == null) {
            // 첫 페이지인 경우
            hasNext = articleIds.size() > PAGE_SIZE;
        } else {
            // 다음 페이지인 경우
            int lastIndex = articleIds.indexOf(lastArticleId);
            hasNext = lastIndex != -1 && (lastIndex + 1 + PAGE_SIZE) < articleIds.size();
        }

        Long nextCursorId = hasNext && !sortedArticles.isEmpty() ?
                sortedArticles.get(sortedArticles.size() - 1).getArticleId() : null;

        return feedConverter.toArticleInFeedDto("맞춤 피드", hasNext, nextCursorId, sortedArticles);
    }

    @Override
    public SearchResponseDto searchArticlesByQuery(String query, int page, int size, double threshold) {
        return nlpService.searchArticles(query, page, size, threshold);
    }

    private ArticleInFeedDto buildFeedResponse(String categoryName, Slice<Article> articleSlice) {
        if (!articleSlice.hasContent()) {
            throw new CustomException(ErrorCode.FEED_9502);
        }
        List<Article> articles = articleSlice.getContent();
        Long nextCursorId = articleSlice.hasNext() ? articles.get(articles.size() - 1).getArticleId() : null;
        return feedConverter.toArticleInFeedDto(categoryName, articleSlice.hasNext(), nextCursorId, articles);
    }

    @Override
    @Transactional
    public void updateUserProfile(UserProfileRequestDto requestDto) {
        nlpService.updateUserProfile(requestDto.getUserId(), requestDto.getInteractions());
    }
}