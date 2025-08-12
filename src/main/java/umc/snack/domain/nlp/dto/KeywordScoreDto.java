package umc.snack.domain.nlp.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @NoArgsConstructor @Setter
public class KeywordScoreDto {
    private String word;
    private double tfidf;
}
