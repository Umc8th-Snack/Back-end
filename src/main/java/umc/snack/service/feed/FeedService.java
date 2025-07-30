package umc.snack.service.feed;

import umc.snack.domain.feed.dto.ArticleInFeedDto;

public interface FeedService {
    ArticleInFeedDto getMainFeedByCategory(String categoryName, Long lastArticleId, Long userId);
}
