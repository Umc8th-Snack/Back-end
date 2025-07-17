package umc.snack.repository.article;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.snack.domain.article.entity.CrawledArticle;

public interface CrawledArticleRepository extends JpaRepository<CrawledArticle, Long> {

    boolean existsByArticleUrl(String articleUrl); // 중복 방지용

    long countByStatus(CrawledArticle.Status status); // 상태별 개수 세기용 (PROCESSED, FAILED 등)



}
