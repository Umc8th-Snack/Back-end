package umc.snack.domain.nlp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter @AllArgsConstructor
public class FeedResponseDto {
    private List<RecommendedArticleDto> articles;
}
