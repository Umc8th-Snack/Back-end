package umc.snack.service.feed;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;
import umc.snack.converter.feed.FeedConverter;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.feed.dto.ArticleInFeedDto;
import umc.snack.repository.feed.CategoryRepository;
import umc.snack.repository.feed.FeedRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedServiceImpl implements FeedService{
    private final FeedRepository feedRepository;
    private final CategoryRepository categoryRepository;
    private final FeedConverter feedConverter;
    private static final int PAGE_SIZE = 16;

    @Override
    public ArticleInFeedDto getMainFeedByCategory(String categoryName, Long lastArticleId, Long userId) {
        // 유효하지 않은 커서 값에 대한 예외처리
        if (lastArticleId != null && lastArticleId <= 0) {
            throw new CustomException(ErrorCode.FEED_9603);
        }
        Pageable pageable = PageRequest.of(0, PAGE_SIZE, Sort.by("publishedAt").descending()
                .and(Sort.by("articleId").descending()));

        Slice<Article> articleSlice;

        // 전체 카테고리에 대해 조회하는 경우
        if ("전체".equals(categoryName)) {
            if (lastArticleId == null)
                articleSlice = feedRepository.findAllArticles(pageable);
            else
                articleSlice = feedRepository.findAllArticlesWithCursor(lastArticleId, pageable);
        } else { // 전체가 아닌, 특정 카테고리에 대해 조회하는 경우
            if (!categoryRepository.existsByCategoryName(categoryName))
                throw new CustomException(ErrorCode.FEED_9601);
            if (lastArticleId == null) {
                articleSlice = feedRepository.findByCategoryName(categoryName, pageable);
            } else {
                articleSlice = feedRepository.findByCategoryNameWithCursor(categoryName, lastArticleId, pageable);
            }
        }

        // 공통 로직을 처리하는 헬퍼 메서드 호출
        return buildFeedResponse(categoryName, articleSlice);
    }
    private ArticleInFeedDto buildFeedResponse(String categoryName, Slice<Article> articleSlice) {
        if (!articleSlice.hasContent())
            throw new CustomException(ErrorCode.FEED_9502);

        List<Article> articles = articleSlice.getContent();
        Long nextCursorId = articleSlice.hasNext() ? articles.get(articles.size() - 1).getArticleId() : null;

        /*
        List<IndividualArticleDto> individualArticles = articles.stream()
                .map(feedConverter::toIndividualArticleDto)
                .toList();

        return ArticleInFeedDto.builder()
                .category(categoryName)
                .hasNext(articleSlice.hasNext())
                .nextCursorId(nextCursorId)
                .articles(individualArticles)
                .build();

         */

        return feedConverter.toArticleInFeedDto(categoryName, articleSlice.hasNext(), nextCursorId, articles);
    }
}
