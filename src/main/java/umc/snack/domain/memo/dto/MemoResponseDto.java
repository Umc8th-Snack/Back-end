package umc.snack.domain.memo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class MemoResponseDto {
    @Builder @Getter @NoArgsConstructor @AllArgsConstructor
    public static class CreateResultDto {
        private Long memoId;
        private String content;
    }

    @Builder @Getter @NoArgsConstructor @AllArgsConstructor
    public static class UpdateResultDto {
        private Long memoId;
        private String content;
    }

    @Builder @Getter @NoArgsConstructor @AllArgsConstructor
    public static class RedirectResultDto {
        private String redirectUrl;
    }

    @Builder @Getter @NoArgsConstructor @AllArgsConstructor
    public static class MemoInfo {
        private Long memoId;
        private String content;
        private LocalDateTime createdAt;
        private String articleUrl;
    }

    @Builder @Getter @NoArgsConstructor @AllArgsConstructor
    public static class MemoListDto {
        private List<MemoInfo> memos;
        private int page;
        private int size;
        private int totalPages;
        private long totalElements;
    }

    @Builder @Getter @NoArgsConstructor @AllArgsConstructor
    public static class ContentDto {
        private String content;
    }
}
