package umc.snack.service.article;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;
import umc.snack.domain.article.dto.ArticleDto;
import umc.snack.domain.article.dto.RelatedArticleDto;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.feed.entity.Category;
import umc.snack.domain.term.dto.TermResponseDto;
import umc.snack.domain.term.entity.ArticleTerm;
import umc.snack.domain.term.entity.Term;
import umc.snack.repository.article.ArticleRepository;
import umc.snack.repository.article.ArticleTermRepository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleService {
    private final ArticleRepository articleRepository;
    private final ArticleTermRepository articleTermRepository;

    @Transactional(readOnly = true)
    public ArticleDto getArticleById(Long articleId) {
        if (articleId == null) {
            throw new CustomException(ErrorCode.ARTICLE_9104_GET);
        }

        // 요약(ready)인 기사만 상세 허용 (NULL/빈문자열 제외)
        Article a = articleRepository.findReadyById(articleId)
                .orElseThrow(() -> new CustomException(ErrorCode.ARTICLE_9104_GET));

        String categoryName = a.getArticleCategories().stream()
                .findFirst()
                .map(ac -> ac.getCategory().getCategoryName())
                .orElse("미분류");

        return ArticleDto.builder()
                .articleId(a.getArticleId())
                .title(a.getTitle())
                .summary(a.getSummary())
                .publishedAt(a.getPublishedAt().toString())
                .articleUrl(a.getArticleUrl())
                .snackUrl("/articles/" + a.getArticleId())
                .category(categoryName)
                .build();
    }

    public List<TermResponseDto> getTermsByArticleId(Long articleId) {
        if (articleId == null) {
            throw new CustomException(ErrorCode.REQ_3102);
        }

        List<ArticleTerm> articleTerms = articleTermRepository.findAllByArticleId(articleId);
        if (articleTerms.isEmpty()) {
            throw new CustomException(ErrorCode.TERM_7101);
        }

        return articleTerms.stream().map(at -> {
            Term term = at.getTerm();
            if (term == null) {
                throw new CustomException(ErrorCode.TERM_7102);
            }
            return new TermResponseDto(
                    term.getWord(),
                    List.of(term.getDefinition()),
                    term.getCreatedAt()
            );
        }).collect(Collectors.toList());
    }

    private static final int RELATED_ARTICLE_COUNT = 3;

    @Cacheable(value = "related-articles", key = "#articleId")
    @Transactional(readOnly = true)
    public List<RelatedArticleDto> findRelatedArticles(Long articleId) {
        if (articleId == null) {
            throw new CustomException(ErrorCode.REQ_3102);
        }

        Article sourceArticle = articleRepository.findById(articleId)
                .orElseThrow(() -> new CustomException(ErrorCode.ARTICLE_9105_RELATED));

        if (sourceArticle.getArticleCategories().isEmpty()) {
            return Collections.emptyList();
        }

        Category targetCategory = sourceArticle.getArticleCategories().get(0).getCategory();

        Pageable pageable = PageRequest.of(
                0, RELATED_ARTICLE_COUNT,
                Sort.by("publishedAt").descending().and(Sort.by("articleId").descending())
        );

        // 관련 기사도 요약(ready)만
        List<Article> relatedArticles = articleRepository.findReadyRelated(
                targetCategory, articleId, pageable
        );

        return relatedArticles.stream()
                .map(RelatedArticleDto::fromEntity)
                .collect(Collectors.toList());
    }
}