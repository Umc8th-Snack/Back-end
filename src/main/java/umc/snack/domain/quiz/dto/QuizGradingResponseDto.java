package umc.snack.domain.quiz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class QuizGradingResponseDto {
    
    private List<QuizGradingDetail> details;
    
    @Getter
    @AllArgsConstructor
    @Builder
    public static class QuizGradingDetail {
        private Long quizId;
        @JsonProperty("isCorrect")
        private boolean isCorrect;
        private int submitted_answer;
        private int answer_index;
        private String description;
    }
} 