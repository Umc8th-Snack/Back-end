package umc.snack.domain.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QuizGradingRequestDto {
    
    private List<SubmittedAnswer> submittedAnswers;
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmittedAnswer {
        private Long quizId;
        private int submitted_answer_index;
    }
} 