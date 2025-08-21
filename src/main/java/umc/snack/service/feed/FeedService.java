package umc.snack.service.feed;

import org.springframework.http.ResponseEntity;
import umc.snack.common.dto.ApiResponse;
import umc.snack.domain.feed.dto.ArticleInFeedDto;
import umc.snack.domain.nlp.dto.SearchResponseDto;
import umc.snack.domain.nlp.dto.UserProfileRequestDto;

import java.util.List;

public interface FeedService {

    ArticleInFeedDto getMainFeedByCategories(List<String> categoryNames, Long lastArticleId, Long userId);
    SearchResponseDto searchArticlesByQuery(String query, int page, int size, String serachMode);
    ArticleInFeedDto getPersonalizedFeed(Long userId, Long lastArticleId);
}
