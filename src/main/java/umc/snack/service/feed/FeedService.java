package umc.snack.service.feed;

import umc.snack.domain.feed.dto.ArticleInFeedDto;
import umc.snack.domain.nlp.dto.SearchResponseDto;

import java.util.List;

public interface FeedService {

    ArticleInFeedDto getMainFeedByCategories(List<String> categoryNames, Long lastArticleId, Long userId);
    SearchResponseDto searchArticlesByQuery(String query, int page, int size, double threshold);
    ArticleInFeedDto getPersonalizedFeed(Long userId, Long lastArticleId);
}
