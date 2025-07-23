package umc.snack.repository.quiz;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import umc.snack.domain.quiz.ArticleQuizId;
import umc.snack.domain.quiz.entity.ArticleQuiz;

import java.util.List;

@Repository
public interface ArticleQuizRepository extends JpaRepository<ArticleQuiz, ArticleQuizId> {
    
    @Query("SELECT aq FROM ArticleQuiz aq JOIN FETCH aq.quiz WHERE aq.articleId = :articleId")
    List<ArticleQuiz> findByArticleIdWithQuiz(@Param("articleId") Long articleId);
} 