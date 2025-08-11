package umc.snack.domain.report.entity;

import jakarta.persistence.*;
import lombok.*;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.user.entity.User;
import umc.snack.global.BaseEntity;


@Entity
@Table(name = "quiz_reports",
        uniqueConstraints = @UniqueConstraint(name = "uk_qr_user_article",
                columnNames = {"user_id","article_id"}),
        indexes = {
                @Index(name = "idx_qr_user", columnList = "user_id"),
                @Index(name = "idx_qr_article", columnList = "article_id")
        })
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor @Builder
public class QuizReport extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_qr_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false, foreignKey = @ForeignKey(name = "fk_qr_article"))
    private Article article;

    @Builder.Default
    @Column(name="is_reported", nullable=false)
    private Boolean isReported = true;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;
}