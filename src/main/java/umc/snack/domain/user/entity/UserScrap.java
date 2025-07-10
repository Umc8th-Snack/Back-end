package umc.snack.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.user.UserScrapId;
import umc.snack.global.BaseEntity;

@Entity
@Table(name = "user_scraps")
@IdClass(UserScrapId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserScrap extends BaseEntity {

    @Id
    @Column(name = "scrap_id")
    private Long scrapId;

    @Id
    @Column(name = "article_id")
    private Long articleId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", insertable = false, updatable = false)
    private Article article;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
}
