package umc.snack.domain.user;

import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserScrapId implements Serializable {
    private Long scrapId;
    private Long articleId;
    private Long userId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserScrapId)) return false;
        UserScrapId that = (UserScrapId) o;
        return Objects.equals(scrapId, that.scrapId) &&
                Objects.equals(articleId, that.articleId) &&
                Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scrapId, articleId, userId);
    }
}