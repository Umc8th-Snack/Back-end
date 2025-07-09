package umc.snack.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import umc.snack.domain.article.entity.Article;
import umc.snack.global.BaseEntity;
import umc.snack.domain.user.UserClicksId;

@Entity
@Table(name = "user_clicks")
@IdClass(UserClicksId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserClicks extends BaseEntity {

    @Id
    @Column(name = "click_id")
    private Long clickId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "article_id")
    private Long articleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", insertable = false, updatable = false)
    private Article article;
}
