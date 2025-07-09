package umc.snack.domain.article.entity;

import jakarta.persistence.*;
import lombok.*;
import umc.snack.domain.article.ArticleCategoryId;
import umc.snack.domain.feed.entity.Category;
import umc.snack.global.BaseEntity;

@Entity
@Table(name = "article_category")
@IdClass(ArticleCategoryId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ArticleCategory extends BaseEntity {

    @Id
    @Column(name = "article_id")
    private Long articleId;

    @Id
    @Column(name = "category_id")
    private Long categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", insertable = false, updatable = false)
    private Article article;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private Category category;
}
