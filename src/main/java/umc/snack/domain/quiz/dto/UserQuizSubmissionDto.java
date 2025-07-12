package umc.snack.domain.quiz.dto;

import lombok.*;

@Getter
@AllArgsConstructor
@Builder
public class UserQuizSubmissionDto {
    private Long resultId;
    private Long userId;
    private Long quizId;
    private String status;
    private String isCorrect;
    private Integer submittedAnswer;
    private java.time.LocalDateTime completedAt;
}
