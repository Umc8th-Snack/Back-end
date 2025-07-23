package umc.snack.repository.quiz;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.snack.domain.quiz.entity.Quiz;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    
}
