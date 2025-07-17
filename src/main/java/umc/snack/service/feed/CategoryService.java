package umc.snack.service.feed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.article.entity.ArticleCategory;
import umc.snack.domain.feed.entity.Category;
import umc.snack.domain.feed.entity.CategoryType;
import umc.snack.repository.article.ArticleCategoryRepository;
import umc.snack.repository.feed.CategoryRepository;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ArticleCategoryRepository articleCategoryRepository;

    /**
     * 기사 URL에서 sectionId를 추출하고 카테고리 매핑
     */
    public void assignCategoryToArticle(Article article, String articleUrl) {
        // 1. sid1 추출
        String sid1 = extractSidFromUrl(articleUrl);
        log.info("articleUrl={}, sid1={}", articleUrl, sid1);

        // 2. sid1 → categoryName 매핑
        String categoryName = CategoryType.fromSid(sid1).getCategoryName();
        log.info("sid1 = {}, categoryName = {}", sid1, categoryName);

        // 3. 트림 처리 및 로그
        String trimmedName = categoryName.trim();
        log.info("🔍 categoryName 요청값 = '{}', 길이 = {}", trimmedName, trimmedName.length());

        // 4. DB에서 카테고리 조회
        Category category = categoryRepository.findByCategoryName(trimmedName)
                .orElseThrow(() ->
                        new IllegalArgumentException("해당 카테고리 이름이 존재하지 않습니다. (입력값: " + trimmedName + ")"));

        // 5. 연관 테이블에 저장
        articleCategoryRepository.save(
                ArticleCategory.builder()
                        .articleId(article.getArticleId())
                        .categoryId(category.getCategoryId())
                        .build()
        );
    }

    /**
     * URL에서 sid 파라미터를 추출합니다 (예: sid=102 → "102").
     */
    private String extractSidFromUrl(String url) {
        Pattern pattern = Pattern.compile("sid=(\\d{3})");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            log.info("✔ sectionId 추출 성공 from URL: {}", matcher.group(1));
            return matcher.group(1);
        }
        log.warn("⚠ URL에서 sectionId를 찾지 못했습니다.");
        return "000";
    }
}