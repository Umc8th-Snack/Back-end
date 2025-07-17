package umc.snack.crawler.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class CrawledArticleDto {
    private Long crawledArticleId;
    private Long articleId;
    private String articleUrl;
    private LocalDateTime publishedAt;
    private String author;
    private String status;
    private String content;
    private LocalDateTime crawledAt;
}
