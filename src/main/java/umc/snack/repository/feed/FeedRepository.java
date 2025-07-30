package umc.snack.repository.feed;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.article.entity.ArticleCategory;

public interface FeedRepository extends JpaRepository<Article, Long> {
    // 메인 피드에서 전체 카테고리 기사 조회
    @Query("SELECT a FROM Article a")
    Slice<Article> findAllArticles(Pageable pageable);

    @Query("SELECT a FROM Article a WHERE a.articleId < :lastArticleId")
    Slice<Article> findAllArticlesWithCursor(@Param("lastArticleId") Long lastArticleId, Pageable pageable);

    @Query("SELECT a FROM Article a JOIN a.articleCategories ac WHERE ac.category.categoryName = :categoryName")
    Slice<Article> findByCategoryName(@Param("categoryName") String categoryName, Pageable pageable);

    @Query("SELECT a FROM Article a JOIN a.articleCategories ac " + "WHERE ac.category.categoryName = :categoryName AND a.articleId < :lastArticleId")
    Slice<Article> findByCategoryNameWithCursor(
            @Param("categoryName") String categoryName, @Param("lastArticleId") Long lastArticleId, Pageable pageable);


}
