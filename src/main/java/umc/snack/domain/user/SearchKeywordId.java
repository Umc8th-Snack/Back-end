package umc.snack.domain.user;

import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchKeywordId implements Serializable {
    private Long searchId;
    private Long userId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SearchKeywordId)) return false;
        SearchKeywordId that = (SearchKeywordId) o;
        return Objects.equals(searchId, that.searchId) &&
                Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(searchId, userId);
    }
}
