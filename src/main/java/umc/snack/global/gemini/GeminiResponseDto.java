package umc.snack.global.gemini;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Getter
public class GeminiResponseDto {
    private List<Candidate> candidates;

    @Getter
    @NoArgsConstructor
    public static class Candidate {
        private Content content;
        private String finishReason;
        private int index;
        List<SafetyRating> safetyRatings;
    }

    @Getter
    @NoArgsConstructor
    public static class Content {
        private List<TextPart> parts;
        private String role;
    }

    @Getter
    @NoArgsConstructor
    public static class TextPart {
        private String text;
    }

    @Getter
    @NoArgsConstructor
    public static class SafetyRating {
        private String category;
        private String probability;
    }
}
