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

    public ArticleDto getArticleById(Long articleId) {
        // articleId가 null인 경우 예외 발생
        if (articleId == null) {
            throw new CustomException(ErrorCode.ARTICLE_9104_GET);
        }

        // articleId로 Article 조회, 없으면 예외 발생
        Article a = articleRepository.findById(articleId)
                .orElseThrow(() -> new CustomException(ErrorCode.ARTICLE_9104_GET));

        // 첫 번째 카테고리 이름 추출, 없으면 "미분류"로 설정
        String categoryName = a.getArticleCategories().stream()
                .findFirst()                            // 단 하나의 매핑 가져오기
                .map(ac -> ac.getCategory().getCategoryName())
                .orElse("미분류");

        // ArticleDto 생성 및 반환
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

    public List<TermResponseDto> getTermsByArticleId(Long articleId) {
        // articleId가 null인 경우 예외 발생
        if (articleId == null) {
            throw new CustomException(ErrorCode.REQ_3102); // Invalid parameter format
        }

        // 해당 articleId에 연결된 용어가 없는 경우 예외 발생
        List<ArticleTerm> articleTerms = articleTermRepository.findAllByArticleId(articleId);
        if (articleTerms.isEmpty()) {
            throw new CustomException(ErrorCode.TERM_7101); // No registered terms
        }

        // 각 용어에 대해 TermResponseDto로 매핑, term이 null이면 예외 발생
        return articleTerms.stream().map(at -> {
            Term term = at.getTerm();
            if (term == null) {
                throw new CustomException(ErrorCode.TERM_7102); // Term not found
            }
            return new TermResponseDto(
                    term.getWord(),
                    List.of(term.getDefinition()),
                    term.getCreatedAt()
            );
        }).collect(Collectors.toList());
    }

    private static final int RELATED_ARTICLE_COUNT = 3; // 관련 기사 개수

    @Cacheable(value="related-articles", key="#articleId")
    @Transactional(readOnly = true)
    public List<RelatedArticleDto> findRelatedArticles(Long articleId) {

        // 0. 파라미터 검증
        if (articleId == null) {
            throw new CustomException(ErrorCode.REQ_3102);
        }

        // 1. 기준 기사 조회
        Article sourceArticle = articleRepository.findById(articleId)
                .orElseThrow(() -> new CustomException(ErrorCode.ARTICLE_9105_RELATED));

        // 2. 기준 기사의 카테고리가 없으면 빈 리스트 반환
        if (sourceArticle.getArticleCategories().isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 기준 카테고리 추출
        Category targetCategory = sourceArticle.getArticleCategories().get(0).getCategory();

        // 4. Pageable 객체 생성
        Pageable pageable = PageRequest.of(0, RELATED_ARTICLE_COUNT,
                Sort.by("publishedAt").descending().and(Sort.by("articleId").descending()));

        // 5. 쿼리 메소드 호출
        List<Article> relatedArticles = articleRepository.findDistinctByArticleCategories_CategoryAndArticleIdNot(
                targetCategory,
                articleId,
                pageable
        );

        // 6. DTO로 변환하여 반환
        return relatedArticles.stream()
                .map(RelatedArticleDto::fromEntity)
                .collect(Collectors.toList());
    }

}