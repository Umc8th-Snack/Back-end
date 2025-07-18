package umc.snack.global.gemini;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/models/")
public interface GeminiInterface {

    @PostExchange("{model}:generateContent")
    GeminiResponseDto.GeminiResponse getCompletion(
            @PathVariable String model,
            @RequestBody GeminiRequestDto.GeminiRequest request
    );
}
