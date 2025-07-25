package umc.snack.domain.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class QuizGradingApiResponse {
    private Boolean isSuccess;
    private String code;
    private String message;
    private Long correctCount;
    private QuizGradingResponseDto result;
} 