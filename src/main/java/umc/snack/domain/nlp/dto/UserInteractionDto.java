package umc.snack.domain.nlp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @JsonInclude(JsonInclude.Include.NON_NULL)
@Builder @NoArgsConstructor @AllArgsConstructor
public class UserInteractionDto {
    private Long articleId;
    private String action;
    private String keyword;

    // 클릭, 스크랩용 생성자
    public UserInteractionDto(Long articleId, String action) {
        this.articleId = articleId;
        this.action = action;
        this.keyword = null;
    }

    // 검색용 생성자
    public UserInteractionDto(String action, String keyword) {
        this.articleId = null;
        this.action = action;
        this.keyword = keyword;
    }
}
