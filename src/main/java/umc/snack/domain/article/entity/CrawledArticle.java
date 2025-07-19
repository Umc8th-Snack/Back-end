package umc.snack.domain.article.entity;

import jakarta.persistence.*;
import lombok.*;
import umc.snack.global.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "crawled_articles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CrawledArticle extends BaseEntity {

    public enum Status {
        PENDING,
        PROCESSED,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "crawled_article_id")
    private Long crawledArticleId;

    @Column(name = "article_id")
    private Long articleId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", insertable = false, updatable = false)
    private Article article;

    @Column(name = "article_url", columnDefinition = "TEXT")
    private String articleUrl;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(length = 100)
    private String author;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "crawled_at")
    private LocalDateTime crawledAt;
}
