package umc.snack.domain.feed.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class IndividualArticleDto {
    private final Long articleId;
    private final String title;
    private final LocalDateTime publishedAt;
    private final String imageUrl;
    private final List<String> categories;
}
