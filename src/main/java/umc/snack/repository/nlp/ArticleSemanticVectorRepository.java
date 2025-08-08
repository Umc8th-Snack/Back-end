package umc.snack.repository.nlp;

import umc.snack.domain.article.entity.ArticleSemanticVector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleSemanticVectorRepository extends JpaRepository<ArticleSemanticVector, Long> {
    // JpaRepository에 이미 findAll(), saveAll() 같은 메서드가 포함되어 있습니다.
    // 따라서 별도의 메서드를 정의할 필요가 없습니다.

    // 만약 articleId로 찾는 메서드가 필요하다면 아래와 같이 정의할 수 있습니다.
    // Optional<ArticleSemanticVector> findByArticleId(Long articleId);
}
