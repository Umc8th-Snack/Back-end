package umc.snack.repository.article;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import umc.snack.domain.article.entity.Article;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    Optional<Article> findByArticleUrl(String articleUrl);
    List<Article> findBySummaryIsNull();

}
