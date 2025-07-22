package umc.snack.domain.memo.dto;

import lombok.Builder;
import lombok.Getter;

public class MemoResponseDto {
    @Builder @Getter
    public static class CreateResultDto {
        private Long memoId;
        private String content;
    }

    @Builder @Getter
    public static class UpdateResultDto {
        private Long memoId;
        private String content;
    }
}
