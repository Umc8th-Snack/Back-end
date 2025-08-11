package umc.snack.repository.article;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.feed.entity.Category;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    Optional<Article> findByArticleUrl(String articleUrl);
//    List<Article> findBySummaryIsNull();
    Page<Article> findBySummaryIsNull(Pageable pageable);

    List<Article> findByArticleCategories_CategoryAndArticleIdNot(Category category, Long articleId, Pageable pageable);
    // articleCategory의 category 이면서(AND) articleId 는 아닌 article을 찾아다오
    // Pageable 객체에 설정된 정렬 순서와 갯수 만큼, List<Article> 형태로!
}

