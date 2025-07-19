package umc.snack.repository.article;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.snack.domain.term.entity.ArticleTerm;

import java.util.Optional;


public interface ArticleTermRepository extends JpaRepository<ArticleTerm, Long> {
    Optional<ArticleTerm> findByArticleIdAndTermId(Long articleId, Long termId);

}
