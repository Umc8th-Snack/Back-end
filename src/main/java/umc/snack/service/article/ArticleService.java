package umc.snack.service.article;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;
import umc.snack.domain.article.dto.ArticleDto;
import umc.snack.domain.article.entity.Article;
import umc.snack.repository.article.ArticleRepository;

@Service
@RequiredArgsConstructor
public class ArticleService {
    private final ArticleRepository articleRepository;

    public ArticleDto getArticleById(Long articleId) {
        Article a = articleRepository.findById(articleId)
                .orElseThrow(() -> new CustomException(ErrorCode.ARTICLE_9104_GET));

        // ArticleCategory 로 연결된 Category에서 첫 번째(단일) 카테고리 이름 추출
        String categoryName = a.getArticleCategories().stream()
                .findFirst()                            // 단 하나의 매핑 가져오기
                .map(ac -> ac.getCategory().getCategoryName())
                .orElse("미분류");

        return ArticleDto.builder()
                .articleId(a.getArticleId())
                .title(a.getTitle())
                .summary(a.getSummary())
                .publishedAt(a.getPublishedAt().toString())
                .articleUrl(a.getArticleUrl())         // 외부 URL
                .snackUrl("/articles/" + a.getArticleId())
                .category(categoryName)                 // 단일 카테고리
                .build();
    }
}