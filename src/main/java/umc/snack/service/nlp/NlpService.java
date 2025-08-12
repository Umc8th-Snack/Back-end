package umc.snack.service.nlp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import jakarta.annotation.PostConstruct;
import org.springframework.web.util.UriComponentsBuilder;
import umc.snack.domain.nlp.dto.FeedResponseDto;
import umc.snack.domain.nlp.dto.SearchResponseDto;
import umc.snack.domain.nlp.dto.UserInteractionDto;
import umc.snack.domain.nlp.dto.UserProfileRequestDto;

import java.net.URI;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NlpService {

    private final RestTemplate restTemplate;

    @Value("${fastapi.url:http://localhost:5000}")
    private String fastapiUrl;

    @PostConstruct
    public void initialize() {
        log.info("🚀 NLP 서비스 초기화 - FastAPI URL: {}", fastapiUrl);
        checkFastApiHealth();
    }

    /**
     * FastAPI 헬스체크
     */
    public boolean checkFastApiHealth() {
        try {
            String healthUrl = fastapiUrl + "/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(healthUrl, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("✅ FastAPI 서버 연결 확인: {}", fastapiUrl);
                return true;
            }
        } catch (Exception e) {
            log.error("❌ FastAPI 서버 연결 실패: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 전체 기사 처리 - FastAPI의 실제 엔드포인트 호출
     */
    public Map<String, Object> processAllArticles(boolean reprocess) {
        log.info("🔄 전체 기사 처리 요청 - 재처리: {}", reprocess);

        try {
            // FastAPI의 실제 엔드포인트: /api/nlp/vectorize/batch
            String url = fastapiUrl + "/api/nlp/vectorize/batch";

            // 쿼리 파라미터 추가
            if (reprocess) {
                url += "?force_update=true&limit=100";
            } else {
                url += "?force_update=false&limit=100";
            }

            log.info("FastAPI 호출: POST {}", url);

            // POST 요청
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> result = response.getBody();
                log.info("✅ FastAPI 응답: {}", result);
                return result;
            } else {
                log.error("FastAPI 응답 오류: {}", response.getStatusCode());
                throw new RuntimeException("FastAPI 처리 실패");
            }

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("FastAPI HTTP 오류: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("FastAPI 호출 실패: " + e.getMessage());
        } catch (Exception e) {
            log.error("FastAPI 호출 중 오류: {}", e.getMessage(), e);
            throw new RuntimeException("FastAPI 호출 실패", e);
        }
    }

    /**
     * 특정 기사들 벡터화
     */
    public Map<String, Object> vectorizeArticles(List<Long> articleIds) {
        log.info("📊 기사 벡터화 요청 - {}개 기사", articleIds.size());

        try {
            // FastAPI의 실제 엔드포인트: /api/nlp/vectorize/articles
            String url = fastapiUrl + "/api/nlp/vectorize/articles";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // FastAPI는 List<Integer>를 받음
            List<Integer> intArticleIds = articleIds.stream()
                    .map(Long::intValue)
                    .toList();

            HttpEntity<List<Integer>> request = new HttpEntity<>(intArticleIds, headers);

            log.info("FastAPI 호출: POST {} with IDs: {}", url, intArticleIds);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> result = response.getBody();
                log.info("✅ 벡터화 완료: {}", result);
                return result;
            } else {
                throw new RuntimeException("FastAPI 벡터화 실패");
            }

        } catch (Exception e) {
            log.error("벡터화 실패: {}", e.getMessage(), e);
            throw new RuntimeException("FastAPI 벡터화 서비스 호출 실패", e);
        }
    }

    /**
     * 통계 조회
     */
    public Map<String, Object> getVectorStatistics() {
        try {
            // FastAPI의 실제 엔드포인트: /api/db/check-schema 또는 커스텀 통계 API
            String url = fastapiUrl + "/api/db/check-schema";

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getBody() != null) {
                return response.getBody();
            }

            // 기본값 반환
            Map<String, Object> stats = new HashMap<>();
            stats.put("fastapi_url", fastapiUrl);
            stats.put("fastapi_status", checkFastApiHealth() ? "connected" : "disconnected");
            return stats;

        } catch (Exception e) {
            log.error("통계 조회 실패: {}", e.getMessage());

            // 오류 시 기본값 반환
            Map<String, Object> stats = new HashMap<>();
            stats.put("fastapi_url", fastapiUrl);
            stats.put("fastapi_status", "error");
            stats.put("error", e.getMessage());
            return stats;
        }
    }

    /**
     * 의미 기반 검색 (수정된 부분)
     * @return SearchResponseDto
     */
    public SearchResponseDto searchArticles(String query, int page, int size, double threshold) {
        log.info("🔍 기사 검색 요청 - 검색어: '{}', 페이지: {}, 크기: {}", query, page, size);

        // URL에 모든 파라미터를 담아서 생성
        /*
        URI uri = UriComponentsBuilder.fromHttpUrl(fastapiUrl)
                .path("/api/articles/search")
                .queryParam("query", query)
                .queryParam("page", page)
                .queryParam("size", size)
                .queryParam("threshold", threshold)
                // .build(true) // 인코딩된 쿼리 파라미터 생성
                .build()
                .toUri();

        log.info("FastAPI 호출: GET {}", uri);

        try {
            ResponseEntity<SearchResponseDto> response = restTemplate.getForEntity(uri, SearchResponseDto.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                SearchResponseDto searchResult = response.getBody();
                log.info("✅ 검색 완료 - 전체: {}개", searchResult.getTotalCount());
                return searchResult;
            } else {
                log.error("FastAPI 검색 실패: {}", response.getStatusCode());
                throw new RuntimeException("FastAPI 검색 서비스 호출에 실패했습니다.");
            }
        } catch (HttpClientErrorException e) {
            log.error("FastAPI 클라이언트 오류 ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("FastAPI 서비스 호출 중 오류 발생");
        }
         */
        // 1. URL 템플릿을 정의합니다. 변수가 들어갈 자리는 {이름}으로 표시합니다.
        String url = fastapiUrl + "/api/articles/search?query={query}&page={page}&size={size}&threshold={threshold}";

        // 2. URL에 들어갈 변수들을 Map으로 정의합니다.
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("query", query);
        uriVariables.put("page", page);
        uriVariables.put("size", size);
        uriVariables.put("threshold", threshold);

        log.info("FastAPI 호출: GET {}, 변수: {}", url, uriVariables);

        try {
            // 3. getForEntity에 URL 템플릿과 변수 Map을 전달합니다.
            // RestTemplate이 'query' 값을 자동으로 안전하게 인코딩합니다.
            ResponseEntity<SearchResponseDto> response = restTemplate.getForEntity(url, SearchResponseDto.class, uriVariables);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                log.error("FastAPI 검색 실패: Status Code {}", response.getStatusCode());
                throw new RuntimeException("FastAPI 검색 서비스가 성공적인 응답을 반환하지 않았습니다.");
            }
        } catch (HttpClientErrorException e) {
            log.error("FastAPI HTTP 오류 ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("FastAPI 서비스 호출 중 HTTP 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("FastAPI 호출 중 알 수 없는 오류 발생: {}", e.getMessage());
            throw new RuntimeException("FastAPI 서비스 호출에 실패했습니다.");
        }
    }


    public void updateUserProfile(Long userId, List<UserInteractionDto> interactions) {
        String url = fastapiUrl + "/api/nlp/user-profile";
        log.info("🚀 FastAPI 사용자 프로필 업데이트 요청: userId={}", userId);

        UserProfileRequestDto requestDto = new UserProfileRequestDto(userId, interactions);
        HttpEntity<UserProfileRequestDto> request = new HttpEntity<>(requestDto);

        try {
            restTemplate.postForObject(url, request, Map.class);
            log.info("✅ FastAPI 사용자 프로필 업데이트 성공: userId={}", userId);
        } catch (Exception e) {
            log.error("❌ FastAPI 사용자 프로필 업데이트 실패: {}", e.getMessage());
            // 에러를 던져서 상위 서비스에서 처리하도록 할 수도 있습니다.
            throw new RuntimeException("FastAPI 사용자 프로필 업데이트 실패", e);
        }
    }

    /**
     * 맞춤 피드 기사 ID 목록 요청
    */
    public FeedResponseDto getPersonalizedFeed(Long userId, int page, int size) {
        log.info("📡 FastAPI 맞춤 피드 요청: userId={}, page={}, size={}", userId, page, size);

        URI uri = UriComponentsBuilder.fromHttpUrl(fastapiUrl)
                .path("/api/nlp/feed/{userId}")
                .queryParam("page", page)
                .queryParam("size", size)
                .buildAndExpand(userId)
                .toUri();

        try {
            FeedResponseDto response = restTemplate.getForObject(uri, FeedResponseDto.class);
            log.info("✅ FastAPI 맞춤 피드 수신 완료: {}개 기사", response.getArticles().size());
            return response;
        } catch (Exception e) {
            log.error("❌ FastAPI 맞춤 피드 요청 실패: {}", e.getMessage());
            // 피드 생성 실패 시 비어있는 응답을 반환하거나 에러를 던질 수 있습니다.
            // return new FeedResponseDto(Collections.emptyList());
            throw new RuntimeException("FastAPI 맞춤 피드 요청 실패", e);
        }
    }
}