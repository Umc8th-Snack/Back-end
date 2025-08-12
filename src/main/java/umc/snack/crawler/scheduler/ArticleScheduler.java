package umc.snack.crawler.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import umc.snack.crawler.service.ArticleCollectorService;
import umc.snack.crawler.service.ArticleCrawlerService;
import umc.snack.service.article.ArticleSummarizeService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleScheduler {

    private final ArticleCollectorService articleCollectorService;
    private final ArticleCrawlerService articleCrawlerService;
    private final ArticleSummarizeService articleSummarizeService;
    private final TaskScheduler taskScheduler;

    /**
     * 서버 시작 시 한 번 자동 크롤링을 트리거합니다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReadyEvent() {
        log.info("🚀 서버 시작 시 자동 기사 크롤링을 실행합니다.");
        try {
            autoCrawl();
        } catch (Exception e) {
            log.error("❌ 서버 시작 자동 크롤링 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    // 오전 기사와 오후 기사를 모두 크롤링하기 위해 하루에 10&18시 2번 크롤링
    // 초(*/30), 분(*), 시(*), 일(*), 월(*), 요일(*)
    //@Scheduled(cron = "*/30 * * * * *", zone = "Asia/Seoul") (테스트용)
    @Scheduled(cron = "0 0 10,18 * * *", zone = "Asia/Seoul")
//    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Seoul")
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




    /**
     * 오전 10시 10분, 오후 6시 10분에 Gemini 요약 실행
     * cron: "0 30 10,18 * * *"
     */
    @Scheduled(cron = "0 10 10,18 * * *", zone = "Asia/Seoul")
//    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Seoul")
    public void autoSummarize() {
        log.info("✅ 스케쥴러 Gemini 기사 요약 시작: {}", LocalDateTime.now());
        try {
            articleSummarizeService.getCompletion();
        } catch (Exception e) {
            log.error("❌ Gemini 기사 요약 중 에러 발생: {}", e.getMessage(), e);
        }
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

//    // 5분 뒤 요약 예약 메서드 추가
//    private void scheduleSummarizeAfter5Min() {
//        log.info("서버 시작 크롤링 완료! Gemini 요약 예약: 5분 뒤 실행 예정 ({})", LocalDateTime.now().plusMinutes(5));
//        taskScheduler.schedule(
//                () -> {
//                    log.info("5분 경과! Gemini 기사 요약 자동 실행 시작 ({})", LocalDateTime.now());
////                    log.info("30초 경과! Gemini 기사 요약 자동 실행 시작 ({})", LocalDateTime.now());
//                    try {
//                        articleSummarizeService.getCompletion();
//                    } catch (Exception e) {
//                        log.error("❌ Gemini 기사 요약 중 에러 발생: {}", e.getMessage(), e);
//                    }
//                },
//                java.util.Date.from(java.time.Instant.now().plusSeconds(300))
//        );
//    }
}