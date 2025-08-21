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
    private String searchMode;

    public static SearchResponseDto empty(String query) {
        return SearchResponseDto.builder()
                .query(query)
                .totalCount(0)
                .articles(new ArrayList<>())
                .searchMode("smart") // 기본값
                .build();
    }
}