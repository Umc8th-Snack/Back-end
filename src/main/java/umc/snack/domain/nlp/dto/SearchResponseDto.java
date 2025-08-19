package umc.snack.domain.nlp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter @NoArgsConstructor
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchResponseDto {
    private String query;
    @JsonIgnoreProperties("total_count")
    private int totalCount;
    private List<ArticleSearchResultDto> articles;

    public static SearchResponseDto empty(String query) {
        return new SearchResponseDto(query, 0, new ArrayList<>());
    }
}