package umc.snack.crawler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import umc.snack.repository.article.CrawledArticleRepository;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleCollectorService {

    private final CrawledArticleRepository crawledArticleRepository;

    // 주요 언론사 OID 목록 (한겨레 포함 8개)
    private static final List<String> NEWS_OIDS = List.of("028", "025", "023", "020", "032", "469", "022", "081");

    // 기사 카테고리(섹션) 코드 - 정치~IT/과학 (네이버 기준)
    private static final List<String> SECTION_CODES = List.of("100", "101", "102", "103", "104", "105");

    private static final String NAVER_PREFIX = "https://n.news.naver.com";

    // ⬇︎ 수집 정책 기본값 (필요 시 여기만 조정)
    private static final int TOTAL_TARGET = 30;     // 배치당 총 수집 개수
    private static final int PER_PUBLISHER_LIMIT = 5; // 언론사별 상한 (0이면 무제한)
    private static final Map<String, Integer> WEIGHTS = Map.of(
            "100", 3, // 정치
            "101", 3, // 경제
            "102", 2, // 사회
            "103", 1, // 생활/문화
            "104", 1, // 세계
            "105", 2  // IT/과학
    );

    // URL 정규화용: mnews/article or article 경로에서 (oid, aid) 추출
    private static final Pattern OID_AID_PATTERN = Pattern.compile("/(?:mnews/)?article/(\\d{3})/(\\d+)");

    /**
     * 외부에서 기본 정책으로 호출되는 엔트리 포인트
     */
    public List<String> collectRandomArticleLinks() {
        int weightSum = WEIGHTS.values().stream().mapToInt(Integer::intValue).sum(); // 12
        // 카테고리별 목표 개수 = TOTAL_TARGET * (해당 가중치 / 가중치합)
        Map<String, Integer> targetPerCat = new HashMap<>();
        int assigned = 0;
        for (String sid1 : SECTION_CODES) {
            int w = WEIGHTS.getOrDefault(sid1, 1);
            int quota = (int) Math.floor((double) TOTAL_TARGET * w / weightSum);
            targetPerCat.put(sid1, quota);
            assigned += quota;
        }
        // 나머지(반올림손실) 분배
        int remain = TOTAL_TARGET - assigned;
        for (String sid1 : SECTION_CODES) {
            if (remain == 0) break;
            targetPerCat.put(sid1, targetPerCat.get(sid1) + 1);
            remain--;
        }

        return collectArticleLinksPerCategoryWeighted(targetPerCat, PER_PUBLISHER_LIMIT);
    }

    /**
     * 카테고리 가중치 타깃 + 언론사 상한 + 배치 중복 제거 + DB 1회 조회
     */
    public List<String> collectArticleLinksPerCategoryWeighted(Map<String, Integer> targetPerCat,
                                                               int perPublisherLimit) {
        // DB에서 기존 수집 URL 1회 조회 후, 모두 정규화 키(oid:aid)로 변환
        Set<String> alreadyCrawledUrlKeys = new HashSet<>();
        for (String url : crawledArticleRepository.findAllArticleUrls()) {
            String key = normalizeUrlKey(url);
            if (key != null) alreadyCrawledUrlKeys.add(key);
        }

        // 배치(이번 실행) 중복 방지용 Set
        Set<String> batchUrlKeys = new HashSet<>();
        Set<String> batchTitleKeys = new HashSet<>();

        Map<String, Integer> pickedPerCat = new HashMap<>();
        Map<String, Integer> pickedPerPublisher = new HashMap<>();
        SECTION_CODES.forEach(s -> pickedPerCat.put(s, 0));

        DateTimeFormatter dateFormatter = DateTimeFormatter.BASIC_ISO_DATE;
        String formattedDate = LocalDate.now().format(dateFormatter);

        // 언론사-섹션 조합을 섞어서 순회
        List<String[]> combos = new ArrayList<>();
        for (String oid : NEWS_OIDS) {
            for (String sid1 : SECTION_CODES) {
                combos.add(new String[]{oid, sid1});
            }
        }
        Collections.shuffle(combos);

        int grandTarget = targetPerCat.values().stream().mapToInt(Integer::intValue).sum(); // TOTAL_TARGET

        List<String> resultLinks = new ArrayList<>();

        outer:
        for (String[] combo : combos) {
            String oid = combo[0];
            String sid1 = combo[1];

            // 카테고리 목표 달성 시 건너뜀
            if (pickedPerCat.get(sid1) >= targetPerCat.getOrDefault(sid1, 0)) continue;
            // 언론사 상한 적용
            if (perPublisherLimit > 0 && pickedPerPublisher.getOrDefault(oid, 0) >= perPublisherLimit) continue;

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

                    // URL 정규화 키 생성 (oid:aid)
                    String urlKey = normalizeUrlKey(articleUrl);
                    if (urlKey == null) continue;

                    // 1) DB에 이미 있는 기사면 스킵
                    if (alreadyCrawledUrlKeys.contains(urlKey)) continue;
                    // 2) 이번 배치에서 이미 뽑은 기사면 스킵
                    if (batchUrlKeys.contains(urlKey)) continue;

                    // 본문 유효성 확인 + 제목키 생성(정규화)
                    TitleAndValidity tv = getNormalizedTitleIfValid(articleUrl);
                    if (tv == null || !tv.valid) continue;

                    // 3) 이번 배치에서 같은 제목(정규화) 이미 있으면 스킵
                    if (tv.normalizedTitleKey != null && batchTitleKeys.contains(tv.normalizedTitleKey)) continue;

                    // 통과 → 채택
                    resultLinks.add(articleUrl);
                    batchUrlKeys.add(urlKey);
                    if (tv.normalizedTitleKey != null) batchTitleKeys.add(tv.normalizedTitleKey);

                    pickedPerCat.put(sid1, pickedPerCat.get(sid1) + 1);
                    pickedPerPublisher.put(oid, pickedPerPublisher.getOrDefault(oid, 0) + 1);

                    // 전체 목표 도달 시 종료
                    if (resultLinks.size() >= grandTarget) break outer;

                    // 조합당 하나만
                    break;
                }

            } catch (IOException e) {
                log.warn("[수집 실패] URL: {}, 오류: {}", listUrl, e.getMessage());
            }
        }

        // 요약 로그
        String catSummary = String.join(", ",
                SECTION_CODES.stream()
                        .map(sid -> String.format("%s=%d/%d", sid,
                                pickedPerCat.getOrDefault(sid, 0),
                                targetPerCat.getOrDefault(sid, 0)))
                        .toList());
        String pubSummary = String.join(", ",
                pickedPerPublisher.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .toList());

        log.info("✅ 카테고리별 수집: {} | 언론사별 수집: {} | 총 {}개", catSummary, pubSummary, resultLinks.size());
        return resultLinks;
    }

    /**
     * 기사 본문 유효성(길이/한글비율) 확인 + 제목 정규화 키 생성
     * - 유효하면 normalizedTitleKey 포함해서 반환, 아니면 null
     */
    private TitleAndValidity getNormalizedTitleIfValid(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(4000)
                    .get();

            // 본문 추출
            String text = doc.select("#dic_area").text();
            if (text.isEmpty()) text = doc.select("#newsEndContents").text();
            if (text.isEmpty()) text = doc.select("article").text();

            if (text == null || text.length() < 50) return new TitleAndValidity(false, null);

            long totalLength = text.length();
            long koreanCharCount = text.chars()
                    .filter(c -> (c >= 0xAC00 && c <= 0xD7A3)   // 완성형
                            || (c >= 0x1100 && c <= 0x11FF)     // 자음
                            || (c >= 0x3130 && c <= 0x318F)     // 호환 자모
                            || (c >= 0xA960 && c <= 0xA97F))    // 확장 자모
                    .count();
            double ratio = (double) koreanCharCount / totalLength;
            if (ratio < 0.6) return new TitleAndValidity(false, null);

            // 제목 추출 및 정규화
            String title = doc.title();
            if (title == null || title.isBlank()) {
                // Naver 모바일 기사 제목이 head<title> 외 위치할 가능성 대비
                String t2 = doc.select("h2.media_end_head_headline, h2#title_area, #title_area").text();
                if (!t2.isBlank()) title = t2;
            }
            String titleKey = normalizeTitleKey(title);

            return new TitleAndValidity(true, titleKey);
        } catch (IOException e) {
            log.debug("기사 유효성/제목 추출 실패: {}", url);
            return null;
        }
    }

    // 제목 정규화: 불용 태그 제거, 공백/기호 정리, 소문자화
    private String normalizeTitleKey(String raw) {
        if (raw == null) return null;
        String t = raw;

        // 흔한 프리픽스 제거
        t = t.replaceAll("^\\s*\\[(속보|단독|종합|영상|포토|머니톡|사설|칼럼|오피니언)\\]\\s*", "");
        // 양끝 특수문구 제거 (사이트명 접미 등)
        t = t.replaceAll("\\s*:\\s*네이버\\s*뉴스\\s*$", "");
        t = t.replaceAll("\\s*:\\s*한겨레\\s*$", "");
        t = t.replaceAll("\\s*:\\s*조선일보\\s*$", "");
        t = t.replaceAll("\\s*:\\s*중앙일보\\s*$", "");
        t = t.replaceAll("\\s*:\\s*동아일보\\s*$", "");
        t = t.replaceAll("\\s*:\\s*서울신문\\s*$", "");
        t = t.replaceAll("\\s*:\\s*매일경제\\s*$", "");
        t = t.replaceAll("\\s*:\\s*한국일보\\s*$", "");

        // 공백/기호 정리
        t = t.replaceAll("[\"'“”‘’]", "");
        t = t.replaceAll("\\s+", " ").trim();
        t = t.toLowerCase(Locale.ROOT);

        return t.isBlank() ? null : t;
    }


    // URL을 (oid:aid) 키로 정규화. 실패 시 null

    private String normalizeUrlKey(String url) {
        try {
            URI u = new URI(url);
            String path = u.getPath();
            Matcher m = OID_AID_PATTERN.matcher(path);
            if (!m.find()) return null;
            String oid = m.group(1);
            String aid = m.group(2);
            return oid + ":" + aid;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    // (옵션) JSON 변환 유틸
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

    // 내부 반환용 DTO
    private record TitleAndValidity(boolean valid, String normalizedTitleKey) {}
}