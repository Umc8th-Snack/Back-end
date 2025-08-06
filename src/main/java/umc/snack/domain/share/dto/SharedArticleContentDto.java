package umc.snack.domain.share.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SharedArticleContentDto {
    //GET 응답의 result 필드용
    private Long articleId;
    private String title;
    private String summary;
    private LocalDateTime publishedAt;
    private String originalUrl;
    private String category;
}
