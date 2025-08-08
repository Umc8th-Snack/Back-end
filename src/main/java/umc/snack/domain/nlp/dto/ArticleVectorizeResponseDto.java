package umc.snack.domain.nlp.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ArticleVectorizeResponseDto {
    private Long articleId;
    private double[] vector;
    private List<ArticleKeywordDto> keywords;
}