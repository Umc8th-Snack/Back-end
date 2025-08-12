package umc.snack.domain.nlp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArticleSearchResultDto {
    @JsonProperty("article_id")
    private Long articleId;
    private String title;
    private String summary;
    private double score;
    private List<KeywordScoreDto> keywords;

    @JsonProperty("published_at")
    private String publishedAt;
}
