package umc.snack.crawler.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import umc.snack.crawler.service.ArticleCrawlerService;

import java.io.IOException;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleScheduler {

    private final ArticleCrawlerService articleCrawlerService;

    // 매일 오전 9시 실행
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void autoCrawl() {
        List<String> links = fetchRecentHaniArticleLinks();
        if (links.isEmpty()) {
            log.warn("한겨레 기사 링크를 가져오지 못했습니다.");
            return;
        }

        // 3개 무작위 선택
        Collections.shuffle(links);
        List<String> selectedLinks = links.stream().limit(3).toList();

        // JSON 형식으로 구성
        StringBuilder jsonBuilder = new StringBuilder("{ \"items\": [ ");
        for (int i = 0; i < selectedLinks.size(); i++) {
            jsonBuilder.append("{ \"link\": \"").append(selectedLinks.get(i)).append("\" }");
            if (i < selectedLinks.size() - 1) {
                jsonBuilder.append(", ");
            }
        }
        jsonBuilder.append(" ] }");

        try {
            articleCrawlerService.crawlFromJson(jsonBuilder.toString());
            log.info("자동 크롤링 완료: {}", selectedLinks);
        } catch (IOException e) {
            log.error("자동 크롤링 중 오류 발생", e);
        }
    }

    // 서버 시작 직후 한 번 실행
    @EventListener(ApplicationReadyEvent.class)
    public void crawlOnceAfterStartup() {
        autoCrawl();
    }

    // 한겨레 최신 기사 링크 수집
    private List<String> fetchRecentHaniArticleLinks() {
        List<String> links = new ArrayList<>();
        String haniUrl = "https://media.naver.com/press/028/ranking?type=popular";

        try {
            Document doc = Jsoup.connect(haniUrl).get();
            Elements elements = doc.select("a[href^=\"https://n.news.naver.com/article/028/\"]");

            for (Element el : elements) {
                String href = el.attr("href").split("\\?")[0]; // ? 뒤 파라미터 제거
                if (!links.contains(href)) {
                    links.add(href);
                }
            }

            log.info("📰 한겨레 기사 링크 {}개 수집", links.size());
        } catch (IOException e) {
            log.error("한겨레 기사 링크 수집 실패", e);
        }

        return links;
    }
}