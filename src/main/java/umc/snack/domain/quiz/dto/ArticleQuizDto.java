package umc.snack.domain.quiz.dto;

import lombok.*;

@Getter
@AllArgsConstructor
@Builder
public class ArticleQuizDto {
    private Long articleId;
    private Long quizId;
}