package umc.snack.domain.term.entity;

import jakarta.persistence.*;
import lombok.*;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.term.ArticleTermId;
import umc.snack.global.BaseEntity;

@Entity
@Table(name = "article_terms")
@IdClass(ArticleTermId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ArticleTerm extends BaseEntity {

    @Id
    @Column(name = "article_id")
    private Long articleId;

    @Id
    @Column(name = "term_id")
    private Long termId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", insertable = false, updatable = false)
    private Article article;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", insertable = false, updatable = false)
    private Term term;
}
