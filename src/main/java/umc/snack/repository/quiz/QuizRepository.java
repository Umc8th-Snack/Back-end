package umc.snack.repository.quiz;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;
import umc.snack.domain.quiz.entity.Quiz;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
} 