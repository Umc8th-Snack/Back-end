/*[사용 중단됨] 복합키 매핑을 위한 식별자 클래스입니다.
원래 UserScrap 엔티티는 scrapId, articleId, userId를 합친 복합키(@IdClass)를 사용했으나,
유지 보수와 단순화를 위해 scrapId를 단일 기본키로 변경하였습니다.
이에 따라 이 클래스는 더 이상 사용되지 않으며, 추후 완전히 삭제될 예정입니다.
(복구가 필요할 경우 Git 기록에서 확인 가능)
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
}*/