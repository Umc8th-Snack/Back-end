package umc.snack.domain.nlp.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ArticleVectorizeRequestDto {
    private Long articleId;
    private String summary;
}