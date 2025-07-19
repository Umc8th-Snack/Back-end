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
import umc.snack.repository.article.ArticleRepository;
import umc.snack.repository.article.CrawledArticleRepository;
import umc.snack.service.feed.CategoryService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

@Transactional
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

                // 기자/특파원 이름 추출 (여러 명 지원)
                List<String> authorList = new java.util.ArrayList<>();
                // 화면에 보이는 기자명 요소 모두 수집
                org.jsoup.select.Elements journalistElements = doc.select(".media_end_head_journalist_name");
                for (Element el : journalistElements) {
                    String text = el.text();
                    // 이메일 주소 제거
                    text = text.replaceAll("\\s*\\S+@\\S+", "");
                    // "파리=유근형 특파원" 처럼 접두부 "=" 뒤만 취하도록
                    int eqIndex = text.indexOf("=");
                    if (eqIndex >= 0) {
                        text = text.substring(eqIndex + 1);
                    }
                    // "기자" 또는 "특파원" 단어 제거
                    text = text.replaceAll("(기자|특파원)$", "").trim();
                    if (!text.isEmpty()) {
                        authorList.add(text);
                    }
                }
                // 만약 기자명이 하나도 없다면 특파원 클래스도 검사
                if (authorList.isEmpty()) {
                    org.jsoup.select.Elements bylineElements = doc.select(".byline_s");
                    for (Element el : bylineElements) {
                        String text = el.text();
                        text = text.replaceAll("\\s*\\S+@\\S+", "");
                        int eqIndex = text.indexOf("=");
                        if (eqIndex >= 0) {
                            text = text.substring(eqIndex + 1);
                        }
                        text = text.replaceAll("(기자|특파원)$", "").trim();
                        if (!text.isEmpty()) {
                            authorList.add(text);
                        }
                    }
                }
                // 리스트로 모인 이름을 comma로 연결
                String author = authorList.isEmpty() ? "" : String.join(", ", authorList);
                log.info("👤 기자/특파원: {}", author);


                // 발행일 = 수정일로 취급, 수정일잉 없을 경우, 발행일로 대체
                String publishedDateStr = doc.select("span._ARTICLE_MODIFY_DATE_TIME").attr("data-modify-date-time");
                if (publishedDateStr.isEmpty()) {
                    publishedDateStr = doc.select("span._ARTICLE_DATE_TIME").attr("data-date-time");
                }

                LocalDateTime publishedAt = null;
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
                categoryService.assignCategoryToArticle(article, link);

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