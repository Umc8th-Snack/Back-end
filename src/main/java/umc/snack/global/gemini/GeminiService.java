package umc.snack.global.gemini;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GeminiService {

    private final GeminiInterface geminiInterface;

    @Autowired
    public GeminiService(GeminiInterface geminiInterface) {
        this.geminiInterface = geminiInterface;
    }

    public String getCompletion(String text, String model) {
        GeminiRequestDto geminiRequest = new GeminiRequestDto(text);
        GeminiResponseDto response = geminiInterface.getCompletion(model, geminiRequest);

        if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
            throw new IllegalStateException("Gemini 응답이 비어있음(candidates null/empty)");
        }

        return response.getCandidates().stream()
                .findFirst()
                .flatMap(candidate -> {
                    if (candidate.getContent() == null || candidate.getContent().getParts() == null || candidate.getContent().getParts().isEmpty()) {
                        return Optional.empty();
                    }
                    return candidate.getContent().getParts().stream().findFirst().map(GeminiResponseDto.TextPart::getText);
                })
                .orElseThrow(() -> new IllegalStateException("Gemini 응답 내용 파싱 실패 (parts null/empty)"));
    }
}
