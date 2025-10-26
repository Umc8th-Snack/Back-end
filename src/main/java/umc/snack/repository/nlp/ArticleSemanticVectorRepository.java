package umc.snack.repository.nlp;

import umc.snack.domain.article.entity.ArticleSemanticVector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArticleSemanticVectorRepository extends JpaRepository<ArticleSemanticVector, Long> {
    Optional<ArticleSemanticVector> findByArticle_ArticleId(Long articleId);
}
