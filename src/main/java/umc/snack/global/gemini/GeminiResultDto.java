package umc.snack.global.gemini;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@AllArgsConstructor
public class GeminiResultDto {

    private String summary;
    private List<QuizDto> quizzes;
    private List<TermDto> terms;

    @Getter @Setter
    @NoArgsConstructor
    public static class QuizDto {
        private String question;
        private List<QuizOptionDto> options;
        private Answer answer;
        private String explanation;
    }

    @Getter @Setter
    @NoArgsConstructor
    public static class QuizOptionDto {
        private int id;
        private String text;
    }

    @Getter @Setter
    @NoArgsConstructor
    public static class TermDto {
        private String word;
        private String meaning;
    }

    @NoArgsConstructor
    @Getter @Setter
    public static class Answer {
        private int id;
        private String text;
    }

}
