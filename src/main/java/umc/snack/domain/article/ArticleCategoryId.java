package umc.snack.domain.article;

import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArticleCategoryId implements Serializable {
    //복합키 클래스
    private Long articleId;
    private Long categoryId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArticleCategoryId)) return false;
        ArticleCategoryId that = (ArticleCategoryId) o;
        return Objects.equals(articleId, that.articleId) &&
                Objects.equals(categoryId, that.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(articleId, categoryId);
    }
}