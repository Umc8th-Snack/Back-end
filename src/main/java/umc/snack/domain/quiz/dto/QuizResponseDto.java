package umc.snack.domain.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class QuizResponseDto {
    private List<QuizContentDto> quizContent;
    
    @Getter
    @AllArgsConstructor
    @Builder
    public static class QuizContentDto {
        private Long quizId;
        private String question;
        private List<String> options;
    }
} 