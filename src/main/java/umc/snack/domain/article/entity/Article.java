package umc.snack.domain.article.entity;

import jakarta.persistence.*;
import lombok.*;
import umc.snack.global.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "articles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Article extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "article_id")
    private Long articleId;

    @Column(columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "article_url", columnDefinition = "TEXT")
    private String articleUrl;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    private Integer viewCount;

    private java.time.LocalDateTime publishedAt;

    @OneToMany(mappedBy = "article", fetch = FetchType.LAZY)
    private List<ArticleCategory> articleCategories = new ArrayList<>();
}