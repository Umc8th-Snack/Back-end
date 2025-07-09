package umc.snack.domain.memo.entity;

import jakarta.persistence.*;
import lombok.*;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.user.entity.User;
import umc.snack.global.BaseEntity;

@Entity
@Table(name = "notes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "article_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Note extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "note_id")
    private Long noteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id")
    private Article article;

    @Column(columnDefinition = "TEXT")
    private String note;
}
