package umc.snack.domain.report.entity;

import jakarta.persistence.*;
import lombok.*;

import umc.snack.domain.article.entity.Article;
import umc.snack.domain.user.entity.User;
import umc.snack.global.BaseEntity;


@Entity
@Table(name = "term_reports",
        uniqueConstraints = @UniqueConstraint(name = "uk_tr_user_article",
                columnNames = {"user_id","article_id"}))
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor @Builder
public class TermReport extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id")
    private Article article;

    @Builder.Default
    @Column(name="is_reported", nullable=false)
    private Boolean isReported = true;
}