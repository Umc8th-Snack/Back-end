package umc.snack.domain.article.dto;

import lombok.*;

@Getter
@AllArgsConstructor
@Builder
public class ArticleTfidfVectorDto {
    private Long tfidfVectorId;
    private Long articleId;
    private String vector;
}