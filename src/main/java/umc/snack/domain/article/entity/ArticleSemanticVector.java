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
    @Column(name = "article_id") // 1. DB의 PK 컬럼명 'article_id'와 일치
    private Long articleId;      // 2. 필드명 변경 (GeneratedValue 삭제됨)

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // 3. 이 관계(Article)를 통해 PK(@Id) 값이 채워진다고 JPA에게 알림
    @JoinColumn(name = "article_id")
    private Article article;

    @Column(name = "vector", columnDefinition = "json") // 4. DB 스키마와 동일하게 "json"으로 변경
    private String vector;

    // 5. DB 스키마에 있는 'representative_vector' 필드 추가
    @Column(name = "representative_vector", columnDefinition = "TEXT")
    private String representativeVector;

    @Column(name = "keywords", columnDefinition = "json") // 6. DB 스키마와 동일하게 "json"으로 변경
    private String keywords;

    @Column(name = "model_version")
    private String modelVersion;
}