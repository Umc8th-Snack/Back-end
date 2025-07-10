package umc.snack.domain.quiz;

import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArticleQuizId implements Serializable {
    private Long articleId;
    private Long quizId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArticleQuizId)) return false;
        ArticleQuizId that = (ArticleQuizId) o;
        return Objects.equals(articleId, that.articleId) &&
                Objects.equals(quizId, that.quizId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(articleId, quizId);
    }
}