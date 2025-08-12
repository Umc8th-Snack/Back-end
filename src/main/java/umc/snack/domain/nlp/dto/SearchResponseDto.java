package umc.snack.domain.nlp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter @NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchResponseDto {
    private String query;
    @JsonIgnoreProperties("total_count")
    private int totalCount;
    private List<ArticleSearchResultDto> articles;
}
