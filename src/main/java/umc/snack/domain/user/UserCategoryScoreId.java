package umc.snack.domain.user;

import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserCategoryScoreId implements Serializable {
    private Long scoreId;
    private Long userId;
    private Long categoryId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserCategoryScoreId)) return false;
        UserCategoryScoreId that = (UserCategoryScoreId) o;
        return Objects.equals(scoreId, that.scoreId) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(categoryId, that.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scoreId, userId, categoryId);
    }
}