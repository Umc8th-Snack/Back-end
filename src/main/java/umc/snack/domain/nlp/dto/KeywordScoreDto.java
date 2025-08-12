package umc.snack.domain.nlp.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @NoArgsConstructor
public class KeywordScoreDto {
    private String word;
    private double tfidf;
}
