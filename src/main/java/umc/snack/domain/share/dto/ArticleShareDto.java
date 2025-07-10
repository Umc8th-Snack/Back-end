package umc.snack.domain.share.dto;

import lombok.*;

@Getter
@AllArgsConstructor
@Builder
public class ArticleShareDto {
    private Long shareId;
    private Long articleId;
    private String shareUuid;
}