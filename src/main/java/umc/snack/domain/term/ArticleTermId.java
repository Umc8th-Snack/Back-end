package umc.snack.domain.term;

import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArticleTermId implements Serializable {
    private Long articleId;
    private Long termId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArticleTermId)) return false;
        ArticleTermId that = (ArticleTermId) o;
        return Objects.equals(articleId, that.articleId) &&
                Objects.equals(termId, that.termId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(articleId, termId);
    }
}