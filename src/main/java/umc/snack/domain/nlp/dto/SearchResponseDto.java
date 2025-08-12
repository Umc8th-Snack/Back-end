package umc.snack.domain.nlp.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter @NoArgsConstructor
public class SearchResponseDto {
    private String query;
    private int totalCount;
    private List<ArticleSearchResultDto> articles;
}
