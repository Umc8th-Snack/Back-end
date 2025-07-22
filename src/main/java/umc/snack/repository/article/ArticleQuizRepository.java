package umc.snack.repository.article;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.snack.domain.quiz.entity.ArticleQuiz;

public interface ArticleQuizRepository extends JpaRepository<ArticleQuiz, Long> {
}
