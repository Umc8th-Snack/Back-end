package umc.snack.global.gemini;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

@Getter
@AllArgsConstructor
public class GeminiResultDto {


    private String summary = "";
    private List<QuizDto> quizzes = Collections.emptyList();
    private List<TermDto> terms = Collections.emptyList();

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizDto {
        private String question = "";
        private List<QuizOptionDto> options = Collections.emptyList();
        private Answer answer = new Answer();
        private String explanation = "";
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizOptionDto {
        private int id;
        private String text = "";
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TermDto {
        private String word = "";
        private String meaning = "";
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Answer {
        private int id;
        private String text = "";
    }

}
