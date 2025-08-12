package umc.snack.repository.feed;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.article.entity.ArticleCategory;

import java.util.List;

public interface FeedRepository extends JpaRepository<Article, Long> {

    // 카테고리별 첫 페이지 조회
    @Query("SELECT DISTINCT a FROM Article a " +
            "LEFT JOIN FETCH a.articleCategories ac " +
            "LEFT JOIN FETCH ac.category " +
            "WHERE ac.category.categoryName IN :categoryNames")
    Slice<Article> findByCategoryName(@Param("categoryNames") List<String> categoryNames, Pageable pageable);

    // 카테고리별 다음 페이지 조회 (커서 기반)
    @Query("SELECT DISTINCT a FROM Article a " +
            "LEFT JOIN FETCH a.articleCategories ac " +
            "LEFT JOIN FETCH ac.category " +
            "WHERE ac.category.categoryName IN :categoryNames AND a.articleId < :lastArticleId")
    Slice<Article> findByCategoryNameWithCursor(
            @Param("categoryNames") List<String> categoryNames, @Param("lastArticleId") Long lastArticleId, Pageable pageable);

    @Query("SELECT DISTINCT a FROM Article a JOIN a.articleCategories ac WHERE ac.category.categoryId IN :categoryIds")
    Slice<Article> findByCategoryId(@Param("categoryIds") List<Long> categoryIds, Pageable pageable);

    @Query("SELECT DISTINCT a FROM Article a JOIN a.articleCategories ac WHERE ac.category.categoryId IN :categoryIds AND a.articleId < :lastArticleId")
    Slice<Article> findByCategoryIdWithCursor(@Param("categoryIds") List<Long> categoryIds, @Param("lastArticleId") Long lastArticleId, Pageable pageable);
}
