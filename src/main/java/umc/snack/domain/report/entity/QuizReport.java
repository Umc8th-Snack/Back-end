package umc.snack.domain.report.entity;

import jakarta.persistence.*;
import lombok.*;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.user.entity.User;
import umc.snack.global.BaseEntity;

@Entity
@Table(
        name = "quiz_reports",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "article_id"})
        },
        indexes = {
                @Index(name = "idx_qr_user", columnList = "user_id"),
                @Index(name = "idx_qr_article", columnList = "article_id"),
                @Index(name = "idx_qr_user_article_reported", columnList = "user_id, article_id, reported")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class QuizReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Column(name = "reported", nullable = false, columnDefinition = "TINYINT(1)")
    @Builder.Default
    private boolean reported = true;

    @Column(name = "reason", length = 1000)
    private String reason;

    public boolean isReported() {
        return reported;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", insertable = false, updatable = false)
    private Article article;
}