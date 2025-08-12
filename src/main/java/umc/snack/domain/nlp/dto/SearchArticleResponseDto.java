package umc.snack.domain.nlp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import umc.snack.domain.article.entity.Article;

import java.util.List;

// 클라이언트에게 최종 검색 결과를 반환하기 위한 응답 DTO
@Getter
@Setter
@NoArgsConstructor
public class SearchArticleResponseDto {
    private List<Article> articles;

    public SearchArticleResponseDto(List<Article> articles) {
        this.articles = articles;
    }

    public int getSize() {
        return articles != null ? articles.size() : 0;
    }

    public boolean isEmpty() {
        return articles == null || articles.isEmpty();
    }
}
