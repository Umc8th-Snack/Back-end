package umc.snack.service.article;

import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;

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

    public void assignCategoryToArticle(Article article, String categoryName) {
        // 유효하지 않은 카테고리 이름인 경우 예외 발생
        Category category = categoryRepository.findByCategoryName(categoryName)
                .orElseThrow(() -> new CustomException(ErrorCode.FEED_9601));

        ArticleCategory articleCategory = ArticleCategory.builder()
                .articleId(article.getArticleId())
                .categoryId(category.getCategoryId())
                .build();

        articleCategoryRepository.save(articleCategory);
    }
}
