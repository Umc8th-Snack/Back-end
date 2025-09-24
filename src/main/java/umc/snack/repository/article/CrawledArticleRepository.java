package umc.snack.repository.article;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import umc.snack.domain.article.entity.CrawledArticle;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CrawledArticleRepository extends JpaRepository<CrawledArticle, Long> {

    boolean existsByArticleUrl(String articleUrl); // 중복 방지용

    long countByStatus(CrawledArticle.Status status); // 상태별 개수 세기용 (PROCESSED, FAILED 등)

    List<CrawledArticle> findByStatus(CrawledArticle.Status status);

    @Query("SELECT c.articleUrl FROM CrawledArticle c") // 이미 크롤링한 URL을 모두 Set으로 가져오는 쿼리
    Set<String> findAllArticleUrls();
    Optional<CrawledArticle> findByArticleId(Long articleId);

    Optional<CrawledArticle> findByArticleIdAndStatus(Long articleId, CrawledArticle.Status status);

}
