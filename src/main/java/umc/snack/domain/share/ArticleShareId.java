package umc.snack.domain.share;

import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArticleShareId implements Serializable {
    private Long shareId;
    private Long articleId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArticleShareId)) return false;
        ArticleShareId that = (ArticleShareId) o;
        return Objects.equals(shareId, that.shareId) &&
                Objects.equals(articleId, that.articleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shareId, articleId);
    }
}
