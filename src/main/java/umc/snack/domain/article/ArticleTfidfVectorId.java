package umc.snack.domain.article;

import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArticleTfidfVectorId implements Serializable {
    private Long tfidfVectorId;
    private Long articleId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArticleTfidfVectorId)) return false;
        ArticleTfidfVectorId that = (ArticleTfidfVectorId) o;
        return Objects.equals(tfidfVectorId, that.tfidfVectorId) &&
                Objects.equals(articleId, that.articleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tfidfVectorId, articleId);
    }
}