package umc.snack.domain.quiz;

import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserQuizResultId implements Serializable {
    private Long resultId;
    private Long userId;
    private Long quizId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserQuizResultId)) return false;
        UserQuizResultId that = (UserQuizResultId) o;
        return Objects.equals(resultId, that.resultId) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(quizId, that.quizId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resultId, userId, quizId);
    }
}
