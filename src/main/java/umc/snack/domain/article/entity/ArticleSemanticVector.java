package umc.snack.domain.article.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import umc.snack.global.BaseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;


@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "article_semantic_vectors")
public class ArticleSemanticVector extends BaseEntity {
    @Id
    @Column(name = "article_id")
    private Long articleId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "article_id")
    private Article article;

    @Column(name = "vector", columnDefinition = "json")
    private String vector;

    @Column(name = "representative_vector", columnDefinition = "TEXT")
    private String representativeVector;

    @Column(name = "keywords", columnDefinition = "json")
    private String keywords;

    @Column(name = "model_version")
    private String modelVersion;
}