package umc.snack.crawler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ArticleCollectorService {

    // 주요 언론사 OID 목록 (한겨레 포함 8개)
    private static final List<String> NEWS_OIDS = List.of("028", "025", "023", "020", "032", "469", "022", "081");

    // 기사 카테고리(섹션) 코드 - 정치~IT/과학 (네이버 기준)
    private static final List<String> SECTION_CODES = List.of("100", "101", "102", "103", "104", "105");

    private static final String NAVER_PREFIX = "https://n.news.naver.com";

    public List<String> collectRandomArticleLinks() {
        Set<String> validLinks = new HashSet<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.BASIC_ISO_DATE;
        String formattedDate = LocalDate.now().format(dateFormatter);
        // 셔플된 언론사+섹션 조합에서 중복 없이 5개 조합만 처리
        List<String[]> combos = new ArrayList<>();
        for (String oid : NEWS_OIDS) {
            for (String sid1 : SECTION_CODES) {
                combos.add(new String[]{oid, sid1});
            }
        }
        Collections.shuffle(combos);
        for (String[] combo : combos) {
            if (validLinks.size() >= 5) break;
            String oid = combo[0];
            String sid1 = combo[1];
            String listUrl = String.format(
                    "https://news.naver.com/main/list.naver?mode=LSD&mid=sec&sid1=%s&oid=%s&date=%s",
                    sid1, oid, formattedDate
            );
            try {
                Document doc = Jsoup.connect(listUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(5000)
                        .get();

                Elements articleLinks = doc.select("a[href*='/article/']");
                log.info("🔍 [{}] 링크 개수: {}", listUrl, articleLinks.size());
                for (Element link : articleLinks) {
                    String href = link.attr("href");
                    String articleUrl = href.startsWith("http") ? href : NAVER_PREFIX + href;
                    String extractedOid = extractOidFromUrl(articleUrl);
                    if (extractedOid == null || !NEWS_OIDS.contains(extractedOid)) continue;
                    if (isValidArticle(articleUrl)) {
                        validLinks.add(articleUrl);
                        break;
                    }
                }
            } catch (IOException e) {
                log.warn("[수집 실패] URL: {}, 오류: {}", listUrl, e.getMessage());
            }
        }

        log.info("✅ 최종 수집된 기사 링크 {}개:\n{}", validLinks.size(), String.join("\n", validLinks));
        return new ArrayList<>(validLinks);
    }

    // 기사 본문이 50자가 남는지 유효성 검사
    private boolean isValidArticle(String url) {
        try {
            Document doc = Jsoup.connect(url).get();
            String text = doc.select("#dic_area, #newsEndContents, article").text();
            return text != null && text.length() > 50; // 50자 이상이면 유효한 기사로 판단
        } catch (IOException e) {
            return false;
        }
    }

    public String toJson(List<String> links) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode root = mapper.createObjectNode();
            ArrayNode items = mapper.createArrayNode();

            for (String link : links) {
                ObjectNode item = mapper.createObjectNode();
                item.put("link", link);
                items.add(item);
            }

            root.set("items", items);
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            log.error("JSON 변환 실패", e);
            return "{\"items\": []}";
        }
    }

    private String extractOidFromUrl(String url) {
        try {
            String path = new URI(url).getPath();
            Matcher m = Pattern.compile("/article/(\\d{3})/").matcher(path);
            return m.find() ? m.group(1) : null;
        } catch (URISyntaxException e) {
            return null;
        }
    }
}