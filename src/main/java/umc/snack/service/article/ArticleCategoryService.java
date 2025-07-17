package umc.snack.service.article;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.article.entity.ArticleCategory;
import umc.snack.domain.feed.entity.Category;
import umc.snack.repository.article.ArticleCategoryRepository;
import umc.snack.repository.feed.CategoryRepository;

@Service
@RequiredArgsConstructor
public class ArticleCategoryService {

    private final CategoryRepository categoryRepository;
    private final ArticleCategoryRepository articleCategoryRepository;

    public void assignCategoryToArticle(Article article, String sid1) {
        Category category = categoryRepository.findByCategoryName(sid1)
                .orElseThrow(() -> new RuntimeException("해당 이름의 카테고리를 찾을 수 없습니다: " + sid1));

        ArticleCategory articleCategory = ArticleCategory.builder()
                .articleId(article.getArticleId())
                .categoryId(category.getCategoryId())
                .build();

        articleCategoryRepository.save(articleCategory);
    }
}
