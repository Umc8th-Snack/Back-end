package umc.snack.crawler;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import umc.snack.crawler.service.ArticleCollectorService;
import umc.snack.crawler.service.ArticleCrawlerService;
import umc.snack.domain.article.entity.CrawledArticle;
import umc.snack.repository.article.CrawledArticleRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

@SpringBootTest
@TestPropertySource(properties = {
        "MYSQL_HOST=ignore-me",
        "MYSQL_PORT=3308",
        "MYSQL_DATABASE=ignore-me",
        "MYSQL_USER=ignore-me",
        "MYSQL_PASSWORD=ignore-me",
        "FASTAPI_URL=http://ignore-me",
        "JWT_SECRET_KEY=test-secret",
        "spring.sql.init.mode=never"
})
@Tag("live")
@ActiveProfiles("test")
public class CrawlQualityTest {

    @Autowired
    private ArticleCrawlerService articleCrawlerService;

    @Autowired
    private ArticleCollectorService articleCollectorService;

    @MockitoSpyBean
    private CrawledArticleRepository crawledArticleRepository;

    @MockitoBean
    private umc.snack.service.nlp.NlpService nlpService;

    @Test
    void crawlDataQualityShouldBeWithinThreshold() throws Exception {
        int sampleSize = Integer.parseInt(System.getProperty("crawl.sample", "200"));
        double maxNullPercent = Double.parseDouble(System.getProperty("crawl.maxNullPercent", "20"));

        ClassPathResource resource = new ClassPathResource("CrawlTest.txt");
        List<String> links;
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            links = br.lines()
                    .filter(line -> line != null && !line.isBlank())
                    .toList();
        }

        links = links.stream().limit(sampleSize).toList();

        if (links.isEmpty()) {
            throw new IllegalStateException("'CrawlTest.txt' 파일이 비어있거나, 파일에 링크가 없습니다.");
        }

        // 2) JSON 생성 후 실제 크롤 실행
        String json = articleCollectorService.toJson(links);

        articleCrawlerService.crawlFromJson(json);

        // 3) 이번 테스트에서 저장된 엔티티 캡처하여 품질 계산
        ArgumentCaptor<CrawledArticle> captor = ArgumentCaptor.forClass(CrawledArticle.class);
        verify(crawledArticleRepository, atLeast(1)).save(captor.capture());

        Set<String> target = new HashSet<>(links);

        List<CrawledArticle> processed = captor.getAllValues().stream()
                .filter(ca -> target.contains(ca.getArticleUrl()))
                .filter(ca -> ca.getStatus() == CrawledArticle.Status.PROCESSED)
                .toList();

        long total = processed.size();
        if (total == 0) {
            throw new AssertionError("❌ PROCESSED 상태로 저장된 아티클이 없습니다. 크롤 결과를 확인하세요.");
        }

        long nullCount = processed.stream()
                .filter(ca -> ca.getContent() == null || ca.getContent().isBlank())
                .count();

        double nullPercent = (double) nullCount / total * 100.0;

        System.out.printf("📊 라이브 총 개수: %d | 빈 값 개수: %d | 빈 값 비율: %.2f%%%n",
                total, nullCount, nullPercent);

        assertTrue(
                nullPercent <= maxNullPercent,
                String.format("❌ 라이브 빈 값 비율 %.2f%% 이(가) 허용 기준 %.2f%% 을(를) 초과했습니다.", nullPercent, maxNullPercent)
        );

        System.out.println("✅ 라이브 크롤 품질이 허용 범위 내에 있습니다.");
    }
}