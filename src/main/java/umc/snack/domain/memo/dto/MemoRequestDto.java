package umc.snack.domain.memo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

public class MemoRequestDto {
    @Getter @Builder
    public static class CreateDto {
        @NotBlank(message = "메모 내용이 비어있습니다.")
        private String content;
    }

    @Getter @Builder
    public static class UpdateDto {
        @NotBlank(message = "메모 내용이 비어있습니다.")
        private String content;
    }
}
