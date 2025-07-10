package umc.snack.domain.article.dto;

import lombok.*;

@Getter
@AllArgsConstructor
@Builder
public class ArticleCategoryDto {
    private Long articleId;
    private Long categoryId;
}

