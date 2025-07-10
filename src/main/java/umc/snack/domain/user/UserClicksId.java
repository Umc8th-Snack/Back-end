package umc.snack.domain.user;

import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserClicksId implements Serializable {
    // 복합키 클래스
    private Long clickId;
    private Long userId;
    private Long articleId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserClicksId)) return false;
        UserClicksId that = (UserClicksId) o;
        return Objects.equals(clickId, that.clickId) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(articleId, that.articleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clickId, userId, articleId);
    }
}