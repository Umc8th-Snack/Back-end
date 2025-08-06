package umc.snack.domain.memo.entity;

import jakarta.persistence.*;
import lombok.*;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.user.entity.User;
import umc.snack.global.BaseEntity;

@Entity
@Table(name = "memos", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "article_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Memo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "memo_id")
    private Long memoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(nullable = false, length = 200)
    private String content;

    // 메모 수정 함수
    public void updateContent(String content) {
        this.content = content;
    }
}
