package umc.snack.domain.quiz.dto;

import lombok.*;

@Getter
@AllArgsConstructor
@Builder
public class QuizDto {
    private Long id;
    private String quizContent;
}