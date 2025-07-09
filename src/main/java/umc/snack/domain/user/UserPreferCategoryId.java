package umc.snack.domain.user;

import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferCategoryId implements Serializable {
    private Long userId;
    private Long categoryId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserPreferCategoryId)) return false;
        UserPreferCategoryId that = (UserPreferCategoryId) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(categoryId, that.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, categoryId);
    }
}