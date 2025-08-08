package umc.snack.domain.nlp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 코사인 유사도 계산 결과를 임시로 저장하기 위한 내부 DTO
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ArticleSimilarityResultDto {
    private Long articleId;
    private double similarity;
}
