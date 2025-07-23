package umc.snack.repository.article;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.snack.domain.article.entity.ArticleCategory;
import umc.snack.domain.article.ArticleCategoryId;

import java.util.List;

public interface ArticleCategoryRepository extends JpaRepository<ArticleCategory, ArticleCategoryId> {
    List<ArticleCategory> findByArticleId(Long articleId);
}
