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
     * 기사 HTML에서 sectionId를 추출하고 카테고리 매핑
     */
    public void assignCategoryToArticle(Article article, String articleUrl, String articleHtml, String currentArticleId) {
        // 1. sid1 추출
        String sid1 = extractSid1FromHtml(articleHtml, currentArticleId);
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
     * HTML 내 script 태그에서 sectionId 추출
     */
    public String extractSid1FromHtml(String html, String currentArticleId) {
        Pattern scriptPattern = Pattern.compile("<script[^>]*>([\\s\\S]*?)</script>", Pattern.CASE_INSENSITIVE);
        Matcher scriptMatcher = scriptPattern.matcher(html);

        Pattern articleIdPattern = Pattern.compile("[\"']?articleId[\"']?\\s*[:=]\\s*[\"']?" +
                Pattern.quote(currentArticleId) + "[\"']?");
        Pattern sectionIdPattern = Pattern.compile("[\"']?sectionId[\"']?\\s*[:=]\\s*[\"']?(\\d{3})[\"']?");

        while (scriptMatcher.find()) {
            String script = scriptMatcher.group(1);

            if (articleIdPattern.matcher(script).find()) {
                Matcher sidMatcher = sectionIdPattern.matcher(script);
                if (sidMatcher.find()) {
                    String sid = sidMatcher.group(1);
                    log.info("✔ sectionId 추출 성공: {}", sid);
                    return sid;
                } else {
                    log.warn("⚠ articleId는 찾았지만, 해당 script 내에 sectionId가 없음");
                }
            }
        }

        log.warn("⚠ HTML 전체에서 sectionId를 찾지 못했습니다.");
        return "000"; // fallback
    }
}