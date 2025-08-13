package umc.snack.repository.report;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.snack.domain.report.entity.QuizReport;

public interface QuizReportRepository extends JpaRepository<QuizReport, Long> {

    boolean existsByUserIdAndArticleId(Long userId, Long articleId);

    boolean existsByUserIdAndArticleIdAndReportedTrue(Long userId, Long articleId);
}