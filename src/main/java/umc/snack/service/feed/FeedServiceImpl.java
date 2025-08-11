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
    public ArticleInFeedDto getMainFeedByCategories(List<String> categoryNames, Long lastArticleId, Long userId) {
        /*
        // 카테고리 단일 선택
        // 유효하지 않은 커서 값에 대한 예외처리
        if (lastArticleId != null && lastArticleId <= 0) {
            throw new CustomException(ErrorCode.FEED_9603);
        }
        Pageable pageable = PageRequest.of(0, PAGE_SIZE, Sort.by("publishedAt").descending()
                .and(Sort.by("articleId").descending()));

        Slice<Article> articleSlice;

        if (!categoryRepository.existsByCategoryName(categoryNames))
            throw new CustomException(ErrorCode.FEED_9601);
        if (lastArticleId == null) {
            articleSlice = feedRepository.findByCategoryName(categoryNames, pageable);
        } else {
            articleSlice = feedRepository.findByCategoryNameWithCursor(categoryNames, lastArticleId, pageable);
        }


        // 공통 로직을 처리하는 헬퍼 메서드 호출
        return buildFeedResponse(categoryNames, articleSlice);
        */

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