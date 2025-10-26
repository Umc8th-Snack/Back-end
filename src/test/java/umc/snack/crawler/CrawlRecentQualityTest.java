package umc.snack.crawler;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import umc.snack.domain.article.entity.CrawledArticle;
import umc.snack.repository.article.CrawledArticleRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled
@SpringBootTest
@Tag("Recent")
public class CrawlRecentQualityTest {

    @Autowired
    private CrawledArticleRepository crawledArticleRepository;

    @Test
    void recentCrawlSuccessRateShouldBeAcceptable() {
        double minSuccessRate = Double.parseDouble(System.getProperty("crawl.minSuccessRate", "70"));
        List<CrawledArticle> recent = crawledArticleRepository
                .findTop60ByOrderByCrawledAtDesc();

        if (recent.isEmpty()) {
            throw new AssertionError("❌ 최근 60개의 크롤링 기록이 없습니다.");
        }

        long processed = recent.stream()
                .filter(c -> c.getStatus() == CrawledArticle.Status.PROCESSED)
                .count();

        double rate = (double) processed / recent.size() * 100.0;

        System.out.printf("📊 최근 60개 중 성공률: %.2f%%%n", rate);
        assertTrue(rate >= minSuccessRate,
                String.format("❌ 성공률 %.2f%% (기준 %.2f%%) 미달", rate, minSuccessRate));

        System.out.println("✅ 최근 크롤링 성공률이 허용 기준을 충족합니다.");
    }
}
