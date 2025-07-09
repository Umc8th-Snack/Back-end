package umc.snack.domain.term.dto;

import lombok.*;

@Getter
@AllArgsConstructor
@Builder
public class ArticleTermDto {
    private Long articleId;
    private Long termId;
}
