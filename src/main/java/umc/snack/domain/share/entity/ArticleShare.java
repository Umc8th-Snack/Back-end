package umc.snack.domain.share.entity;

import jakarta.persistence.*;
import lombok.*;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.share.ArticleShareId;
import umc.snack.global.BaseEntity;

@Entity
@Table(name = "article_shares")
@IdClass(ArticleShareId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ArticleShare extends BaseEntity {

    @Id
    @Column(name = "share_id")
    private Long shareId;

    @Id
    @Column(name = "article_id")
    private Long articleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", insertable = false, updatable = false)
    private Article article;

    @Column(name = "share_uuid", length = 36)
    private String shareUuid;
}
