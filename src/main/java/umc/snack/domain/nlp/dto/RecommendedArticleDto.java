package umc.snack.domain.nlp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class RecommendedArticleDto {
    private Long articleId;
    private double score;
}
