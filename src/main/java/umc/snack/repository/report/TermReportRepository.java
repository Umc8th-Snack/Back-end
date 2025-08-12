package umc.snack.repository.report;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.snack.domain.report.entity.TermReport;

public interface TermReportRepository extends JpaRepository<TermReport, Long> {
    boolean existsByUser_UserIdAndArticle_ArticleId(Long userId, Long articleId);
}


