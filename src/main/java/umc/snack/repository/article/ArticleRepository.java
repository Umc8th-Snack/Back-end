package umc.snack.repository.article;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.feed.entity.Category;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    // 공통
    Optional<Article> findByArticleUrl(String articleUrl);

    // --- 배치용: 요약이 아직 없는 기사만 (Gemini 요약 생성 대상) ---
    Page<Article> findBySummaryIsNull(Pageable pageable);

    // --- 노출용(READY): 요약이 null도 아니고 빈 문자열도 아닌 기사만 ---

    // 상세 조회
    @Query("SELECT a FROM Article a " +
            "WHERE a.articleId = :articleId " +
            "AND a.summary IS NOT NULL AND a.summary <> ''")
    Optional<Article> findReadyById(@Param("articleId") Long articleId);

    // 목록 조회 (필요 시 사용)
    @Query("SELECT a FROM Article a " +
            "WHERE a.summary IS NOT NULL AND a.summary <> ''")
    Page<Article> findReady(Pageable pageable);

    // 관련 기사 조회
    @Query("""
           SELECT DISTINCT a
           FROM Article a
           JOIN a.articleCategories ac
           WHERE ac.category = :category
             AND a.articleId <> :articleId
             AND a.summary IS NOT NULL
             AND a.summary <> ''
           """)
    List<Article> findReadyRelated(@Param("category") Category category,
                                   @Param("articleId") Long articleId,
                                   Pageable pageable);

    // (레거시 유지: 필요 시 다른 경로에서 사용)
    List<Article> findDistinctByArticleCategories_CategoryAndArticleIdNot(
            Category category, Long articleId, Pageable pageable
    );

    @Query("SELECT a FROM Article a " +
            "WHERE a.articleId IN :ids " +
            "AND a.summary IS NOT NULL AND a.summary <> ''")
    List<Article> findAllReadyByIdIn(@Param("ids") List<Long> ids);
}