package umc.snack.service.feed;

import umc.snack.domain.feed.dto.ArticleInFeedDto;

import java.util.List;

public interface FeedService {

    ArticleInFeedDto getMainFeedByCategories(List<String> categoryNames, Long lastArticleId, Long userId);
}
