package umc.snack.domain.feed.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Builder
public class ArticleInFeedDto {
    private final Long articleId;
    private final String title;
    private final LocalDateTime publishedAt;
    private final String imageUrl;
    private final String category;  // 카테고리 이름
    private final boolean hasNext;
    private final List<IndividualArticleDto> articles;
    private final List<String> categories;
    private final Long nextCursorId;
}