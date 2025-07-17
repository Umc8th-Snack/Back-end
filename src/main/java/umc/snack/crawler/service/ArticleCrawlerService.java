package umc.snack.crawler.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.article.entity.CrawledArticle;
import umc.snack.repository.article.ArticleCategoryRepository;
import umc.snack.repository.article.ArticleRepository;
import umc.snack.repository.article.CrawledArticleRepository;
import umc.snack.service.feed.CategoryService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleCrawlerService {

    private final CrawledArticleRepository crawledArticleRepository;
    private final ArticleRepository articleRepository;
    private final CategoryService categoryService;

    private static final String NAVER_PREFIX = "https://n.news.naver.com";

    public void crawlFromJson(String json) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(json);
        JsonNode items = rootNode.path("items");

        for (JsonNode item : items) {
            String link = item.path("link").asText();

            if (!link.startsWith(NAVER_PREFIX)) continue;
            if (crawledArticleRepository.existsByArticleUrl(link)) continue;

            try {
                Document doc = Jsoup.connect(link)
                        .userAgent("Mozilla/5.0")
                        .get();

                String content = doc.select("#dic_area").text();
                if (content.isEmpty()) content = doc.select("div#newsEndContents").text();
                if (content.isEmpty()) content = doc.select("article").text();
                if (content.isEmpty()) content = doc.body().text();

                log.info("[크롤링 결과] link: {}\ncontent: {}", link, content);

                // 기자 이름 추출
                String author = "";
                Element journalistElement = doc.selectFirst(".media_end_head_journalist_name");
                if (journalistElement != null) {
                    author = journalistElement.text().replace("기자", "").trim();
                }
                log.info("👤 기자: {}", author);        // 이정하


                // 발행일 = 수정일로 취급
                LocalDateTime publishedAt = null;
                String publishedDateStr = doc.select("._ARTICLE_MODIFY_DATE_TIME").attr("data-modify-date-time");
                if (!publishedDateStr.isEmpty()) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    publishedAt = LocalDateTime.parse(publishedDateStr, formatter);
                }

                log.info("🗓️ 발행일(=수정일): {}", publishedAt);  // 2025-07-17T15:24:17

                // 이 시점에서 둘 다 final 로 선언 (람다에서 사용 가능)
                final String finalAuthor = author;
                final LocalDateTime finalPublishedAt = publishedAt;

                String originalTitle = doc.title();
                String title = originalTitle.length() > 100 ? originalTitle.substring(0, 100) : originalTitle;

                Article article = articleRepository.findByArticleUrl(link)
                        .orElseGet(() -> articleRepository.save(
                                Article.builder()
                                        .title(title)
                                        .articleUrl(link)
                                        .publishedAt(finalPublishedAt)
                                        .viewCount(0)
                                        .build()
                        ));

                String html = doc.outerHtml();
                String articleId = link.substring(link.lastIndexOf("/") + 1);

                // 카테고리 할당 (sid1은 내부에서 추출됨)
                categoryService.assignCategoryToArticle(article, link, html, articleId);

                CrawledArticle crawledArticle = CrawledArticle.builder()
                        .articleUrl(link)
                        .author(author.isEmpty() ? "unknown" : author)
                        .publishedAt(finalPublishedAt)
                        .content(content)
                        .status(CrawledArticle.Status.PROCESSED)
                        .crawledAt(LocalDateTime.now())
                        .articleId(article.getArticleId())
                        .build();

                crawledArticleRepository.save(crawledArticle);

            } catch (Exception e) {
                crawledArticleRepository.save(
                        CrawledArticle.builder()
                                .articleUrl(link)
                                .status(CrawledArticle.Status.FAILED)
                                .crawledAt(LocalDateTime.now())
                                .build()
                );

                log.warn("[크롤링 실패] {} : {}", link, e.getMessage(), e);
            }
        }
    }
}