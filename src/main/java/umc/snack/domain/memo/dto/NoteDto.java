package umc.snack.domain.memo.dto;

import lombok.*;

@Getter
@AllArgsConstructor
@Builder
public class NoteDto {
    private Long noteId;
    private Long userId;
    private Long articleId;
    private String note;
}
