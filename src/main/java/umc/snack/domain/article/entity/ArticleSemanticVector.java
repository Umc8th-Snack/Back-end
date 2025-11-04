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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vector_id")
    private Long vectorId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false, unique = true)
    private Article article;

    @Column(name = "vector", columnDefinition = "TEXT")
    private String vector;  // JSON 형태로 저장된 벡터

    @Column(name = "keywords", columnDefinition = "TEXT")
    private String keywords;  // 키워드와 점수를 문자열로 저장

    @Column(name = "model_version")
    private String modelVersion;
}
