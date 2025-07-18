package umc.snack.global.gemini;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GeminiService {

    private final GeminiInterface geminiInterface;

    @Autowired
    public GeminiService(GeminiInterface geminiInterface) {
        this.geminiInterface = geminiInterface;
    }

    public String getCompletion(String text, String model) {
        GeminiRequestDto.GeminiRequest geminiRequest = new GeminiRequestDto.GeminiRequest(text);
        GeminiResponseDto.GeminiResponse response = geminiInterface.getCompletion(model, geminiRequest);

        return response.getCandidates()
                .stream()
                .findFirst()
                .flatMap(candidate -> candidate.getContent().getParts()
                        .stream()
                        .findFirst()
                        .map(GeminiResponseDto.GeminiResponse.TextPart::getText))
                .orElse(null);
    }
}
