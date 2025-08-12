package umc.snack.domain.report.entity;

import jakarta.persistence.*;
import lombok.*;

import umc.snack.domain.article.entity.Article;
import umc.snack.domain.user.entity.User;
import umc.snack.global.BaseEntity;


@Entity
@Table(name = "term_reports",
        uniqueConstraints = @UniqueConstraint(name = "uk_tr_user_article",
                columnNames = {"user_id","article_id"}),
        indexes = {
            @Index(name = "idx_tr_user", columnList = "user_id"),
            @Index(name = "idx_tr_article", columnList = "article_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TermReport extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tr_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tr_article"))
    private Article article;

    @Builder.Default
    @Column(name = "reported", nullable = false, columnDefinition = "TINYINT(1)")
    private boolean reported = true;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    // Explicit accessors to ensure service layer calls compile
    public boolean isReported() {
        return reported;
    }

    public void setReported(boolean reported) {
        this.reported = reported;
    }

    /** Allow setting via 0/1 semantics if needed */
    public void setReportedInt(int value) {
        this.reported = (value == 1);
    }
}