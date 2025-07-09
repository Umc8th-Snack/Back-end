package umc.snack.domain.article.entity;

import jakarta.persistence.*;
import lombok.*;
import umc.snack.domain.article.ArticleTfidfVectorId;
import umc.snack.global.BaseEntity;

@Entity
@Table(name = "article_tfidf_vectors")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ArticleTfidfVector extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tfidf_vector_id")
    private Long tfidfVectorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id")
    private Article article;

    @Column(columnDefinition = "TEXT")
    private String vector;
}

