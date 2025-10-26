package umc.snack.crawler;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import umc.snack.crawler.service.ArticleCollectorService;
import umc.snack.crawler.service.ArticleCrawlerService;
import umc.snack.domain.article.entity.CrawledArticle;
import umc.snack.repository.article.CrawledArticleRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Tag("live")
public class CrawlQualityTest {

    @Autowired
    private ArticleCrawlerService articleCrawlerService;

    @Autowired
    private ArticleCollectorService articleCollectorService;

    @MockitoSpyBean
    private CrawledArticleRepository crawledArticleRepository;

    @Test
    void crawlDataQualityShouldBeWithinThreshold() throws Exception {
        int sampleSize = Integer.parseInt(System.getProperty("crawl.sample", "500"));
        double maxNullPercent = Double.parseDouble(System.getProperty("crawl.maxNullPercent", "20"));

        // 1) DB에서 최신 기사 URL 수집 (카테고리별 10개, IT/과학 가중 2, 언론사 상한 3)
        java.util.Map<String, Integer> weights = new java.util.HashMap<>();
        weights.put("100", 1);
        weights.put("101", 1);
        weights.put("102", 1);
        weights.put("103", 1);
        weights.put("104", 1);
        weights.put("105", 2);
        int perPublisherLimit = 3;

        List<String> links = articleCollectorService.collectArticleLinksPerCategoryWeighted(10, weights, perPublisherLimit)
                .stream()
                .limit(sampleSize)
                .toList();

        if (links.isEmpty()) {
            throw new IllegalStateException("DB/수집기에서 가져온 신규 기사 링크가 없습니다. 수집 로직 혹은 네트워크 상태를 확인하세요.");
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