package umc.snack.crawler.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import umc.snack.crawler.service.ArticleCollectorService;
import umc.snack.crawler.service.ArticleCrawlerService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleScheduler {

    private final ArticleCollectorService articleCollectorService;
    private final ArticleCrawlerService articleCrawlerService;

    // 가장 많은 기사를 수집하기 위해 23시 30분에 자동 크롤링
    // 초(*/30), 분(*), 시(*), 일(*), 월(*), 요일(*)
    //@Scheduled(cron = "*/30 * * * * *", zone = "Asia/Seoul") (테스트용)
    @Scheduled(cron = "0 30 8,14,23 * * *", zone = "Asia/Seoul")
    public void autoCrawl() {
        log.info("✅ 스케쥴러 실행 확인 > {}", LocalDateTime.now());
        try {
            List<String> links = articleCollectorService.collectRandomArticleLinks(); // 링크 수집
            String json = articleCollectorService.toJson(links);                         // JSON 변환
            articleCrawlerService.crawlFromJson(json);                                   // 크롤링 실행
        } catch (IOException e) {
            System.err.println("❌ 자동 크롤링 중 오류 발생: " + e.getMessage());
        }
    }

    // 서버 기동 직후 1회 실행
    @EventListener(ApplicationReadyEvent.class)
    public void crawlOnceAfterStartup() {
        crawlArticles();
    }

    private void crawlArticles() {
        try {
            List<String> links = articleCollectorService.collectRandomArticleLinks(); // 링크 수집
            String json = articleCollectorService.toJson(links);                           // JSON 변환
            articleCrawlerService.crawlFromJson(json);                                     // 크롤링 실행
        } catch (IOException e) {
            System.err.println("❌ 자동 크롤링 중 오류 발생: " + e.getMessage());
        }
    }
}