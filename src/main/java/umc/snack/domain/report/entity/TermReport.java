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
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor @Builder
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
    @Column(name="reported", nullable=false)
    private Boolean reported = true;

    // Temporary mirror to satisfy legacy column 'is_reported' if present in DB
    @Column(name = "is_reported", nullable = false)
    private Boolean isReportedMirror;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @PrePersist
    public void syncReportedBeforeInsert() {
        if (reported == null) {
            reported = true;
        }
        isReportedMirror = reported;
    }

    @PreUpdate
    public void syncReportedBeforeUpdate() {
        isReportedMirror = reported;
    }
}