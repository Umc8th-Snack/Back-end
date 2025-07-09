package umc.snack.domain.article.dto;

import lombok.*;

@Getter
@AllArgsConstructor
@Builder
public class ArticleDto {
    private Long articleId;
    private String title;
    private String summary;
    private String articleUrl;
    private String imageUrl;
    private Integer viewCount;
    private String publishedAt;
}