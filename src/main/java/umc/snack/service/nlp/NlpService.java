package umc.snack.service.nlp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import jakarta.annotation.PostConstruct;
import org.springframework.web.util.UriComponentsBuilder;
import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;
import umc.snack.domain.nlp.dto.*;
import org.springframework.scheduling.annotation.Async;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.*;

import static org.springframework.http.HttpStatus.*;

@Slf4j
@Service
public class NlpService {

    private final RestTemplate fastApiRestTemplate;
    private final RestTemplate longTimeoutRestTemplate;
    private final String fastapiUrl;

    // application.yml 파일에서 fastapi.url 가져오기!!
    public NlpService(@Qualifier("fastApiRestTemplate") RestTemplate fastApiRestTemplate,
                      @Qualifier("longTimeoutRestTemplate") RestTemplate longTimeoutRestTemplate,
                      @Value("${fastapi.url}") String fastapiUrl){
        this.fastApiRestTemplate = fastApiRestTemplate;
        this.longTimeoutRestTemplate = longTimeoutRestTemplate;
        this.fastapiUrl = fastapiUrl;
    }

    @PostConstruct
    public void initialize() {
        log.info("NLP 서비스 초기화 - FastAPI URL: {}", fastapiUrl);
    }

    /**
     * FastAPI 헬스체크
     */
    @GetMapping("/health")
    public NlpResponseDto.HealthCheckDto healthCheck() {
        try {
            String healthUrl = fastapiUrl + "/health";
            ResponseEntity<Map> response = fastApiRestTemplate.getForEntity(healthUrl, Map.class);

            boolean isHealthy = (response.getStatusCode() == HttpStatus.OK);

            return NlpResponseDto.HealthCheckDto.builder()
                    .fastapi_status(isHealthy ? "connected" : "disconnected")
                    .build();
        } catch (ResourceAccessException e) {
            throw new CustomException(ErrorCode.SERVER_5102);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.SERVER_5101);
        }
    }

    /**
     * 전체 기사 벡터화 - FastAPI의 실제 엔드포인트 호출
     */
    @Async("taskExecutor")
    public void processAllArticles(boolean reprocess) {
        log.info("비동기 전체 기사 처리 시작 - 재처리: {}", reprocess);

        int batchSize = 50;
        int totalProcessed = 0;
        boolean hasMoreArticles = true;

        HttpEntity<Void> request = new HttpEntity<>(new HttpHeaders());

        try {
            while (hasMoreArticles) {

                String url = String.format("%s/api/nlp/vectorize/batch?limit=%d&force_update=%b",
                        fastapiUrl, batchSize, reprocess);

                log.info("FastAPI 배치 요청: ({}개)", batchSize);

                ResponseEntity<Map> response = longTimeoutRestTemplate.postForEntity(url, request, Map.class);
                Map<String, Object> body = response.getBody();

                if (body == null || "no_articles".equals(body.get("status"))) {
                    hasMoreArticles = false;
                    log.info("처리할 기사가 더 이상 없습니다. 루프를 종료합니다.");

                } else {
                    int processedInThisBatch = (int) body.getOrDefault("processed", 0);
                    totalProcessed += processedInThisBatch;
                    log.info("이번 배치에서 {}개 처리 완료 (총 {}개 처리)", processedInThisBatch, totalProcessed);

                    if (processedInThisBatch < batchSize) {
                        hasMoreArticles = false;
                        log.info("마지막 배치를 완료했습니다.");
                    }
                }

                if (hasMoreArticles) {
                    Thread.sleep(1000);
                }
            }

            log.info("비동기 전체 기사 처리 완료. 총 {}개 기사 처리됨.", totalProcessed);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("기사 처리 스레드가 중단되었습니다.", e);
        } catch (Exception e) {
            log.error("비동기 기사 처리 중 심각한 오류 발생", e);
        }
    }

    /**
     * 특정 기사들 벡터화
     */
    public Map<String, Object> vectorizeArticles(List<Long> articleIds) {
        log.info("기사 벡터화 요청 - {}개 기사", articleIds.size());
        String url = fastapiUrl + "/api/vectorize/articles";
        List<Integer> intArticleIds = articleIds.stream().map(Long::intValue).toList();
        HttpEntity<List<Integer>> request = new HttpEntity<>(intArticleIds);

        try {
            ResponseEntity<Map> response = longTimeoutRestTemplate.postForEntity(url, request, Map.class);
            if (response.getBody() != null) {
                log.info("벡터화 완료: {}", response.getBody());
                return response.getBody();
            }
            throw new CustomException(ErrorCode.NLP_9806); // 벡터 계산 오류
        } catch (ResourceAccessException e) {
            log.error("FastAPI 연결 시간 초과: {}", url, e);
            throw new CustomException(ErrorCode.SERVER_5102);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("FastAPI HTTP 오류: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomException(ErrorCode.NLP_9806);
        } catch (Exception e) {
            log.error("벡터화 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.SERVER_5101);
        }
    }

    /**
     * 의미 기반 검색
     */
    public SearchResponseDto searchArticles(String cleanedQuery, int page, int size, double threshold) {

        if (!StringUtils.hasText(cleanedQuery)) {
            throw new CustomException(ErrorCode.NLP_9807);
        }

        log.info("FastAPI 검색 요청 - 정리된 검색어: '{}', 페이지: {}, 크기: {}", cleanedQuery, page, size);

        try {
            String encodedQuery = URLEncoder.encode(cleanedQuery, "UTF-8");
            String url = String.format("%s/api/articles/search?query=%s&page=%d&size=%d&threshold=%.1f",
                    fastapiUrl, encodedQuery, page, size, threshold);

            log.info("FastAPI 호출 URL: {}", url);

            URI uri = URI.create(url);

            ResponseEntity<SearchResponseDto> response = fastApiRestTemplate.getForEntity(uri, SearchResponseDto.class);

            if (response.getBody() == null) {
                log.warn("FastAPI 응답 본문이 null - 검색어: '{}'", cleanedQuery);
                throw new CustomException(ErrorCode.NLP_9808);
            }

            SearchResponseDto result = response.getBody();

            if (result.getArticles() == null || result.getArticles().isEmpty()) {
                log.info("검색 결과 없음 - 검색어: '{}', 전체 개수: {}", cleanedQuery, result.getTotalCount());
                return result;
            }

            log.info("FastAPI 검색 성공 - 검색어: '{}', 결과: {}/{}", cleanedQuery, result.getArticles().size(), result.getTotalCount());
            return result;

        } catch (UnsupportedEncodingException e) {
            log.error("URL 인코딩 실패 - 검색어: '{}', 오류: {}", cleanedQuery, e.getMessage());
            throw new CustomException(ErrorCode.REQ_3102);

        } catch (ResourceAccessException e) {
            log.error("FastAPI 연결 시간 초과 - URL: {}, 오류: {}", fastapiUrl, e.getMessage());
            throw new CustomException(ErrorCode.FEED_9606);

        } catch (HttpClientErrorException e) {
            log.error("FastAPI 클라이언트 오류 - 상태: {}, 응답: {}", e.getStatusCode(), e.getResponseBodyAsString());

            HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());

            switch (status) {
                case BAD_REQUEST:
                    throw new CustomException(ErrorCode.NLP_9801);
                case NOT_FOUND:
                    log.info("FastAPI에서 검색 결과 없음 반환 - 검색어: '{}'", cleanedQuery);
                    throw new CustomException(ErrorCode.NLP_9808);
                case UNPROCESSABLE_ENTITY:
                    throw new CustomException(ErrorCode.NLP_9807);
                default:
                    throw new CustomException(ErrorCode.NLP_9899);
            }

        } catch (HttpServerErrorException e) {
            log.error("FastAPI 서버 오류 - 상태: {}, 응답: {}", e.getStatusCode(), e.getResponseBodyAsString());

            HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());

            switch (status) {
                case SERVICE_UNAVAILABLE:
                    throw new CustomException(ErrorCode.SERVER_5103);
                case GATEWAY_TIMEOUT:
                    throw new CustomException(ErrorCode.SERVER_5102);
                default:
                    throw new CustomException(ErrorCode.NLP_9899);
            }

        } catch (CustomException e) {
            throw e;

        } catch (Exception e) {
            log.error("FastAPI 검색 호출 중 예상치 못한 오류 - 검색어: '{}', 오류: {}", cleanedQuery, e.getMessage(), e);
            throw new CustomException(ErrorCode.SERVER_5101);
        }
    }

    public void updateUserProfile(Long userId, List<UserInteractionDto> interactions) {
        String url = fastapiUrl + "/api/nlp/user-profile";
        log.info("FastAPI 사용자 프로필 업데이트 요청: userId={}", userId);
        HttpEntity<UserProfileRequestDto> request = new HttpEntity<>(new UserProfileRequestDto(userId, interactions));

        try {
            fastApiRestTemplate.postForEntity(url, request, Map.class);
            log.info("FastAPI 사용자 프로필 업데이트 성공: userId={}", userId);
        } catch (ResourceAccessException e) {
            log.error("FastAPI 연결 시간 초과: {}", url, e);
            throw new CustomException(ErrorCode.SERVER_5102);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("FastAPI 프로필 업데이트 HTTP 오류: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomException(ErrorCode.NLP_9899);
        } catch (Exception e) {
            log.error("FastAPI 프로필 업데이트 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.SERVER_5101);
        }
    }

    public FeedResponseDto getPersonalizedFeed(Long userId, int page, int size) {
        log.info("FastAPI 맞춤 피드 요청: userId={}, page={}, size={}", userId, page, size);
        URI uri = UriComponentsBuilder.fromHttpUrl(fastapiUrl)
                .path("/api/nlp/feed/{userId}")
                .queryParam("page", page)
                .queryParam("size", size)
                .buildAndExpand(userId)
                .toUri();

        try {
            ResponseEntity<FeedResponseDto> responseEntity = fastApiRestTemplate.getForEntity(uri, FeedResponseDto.class);
            FeedResponseDto response = responseEntity.getBody();

            if (response == null || response.getArticles() == null) {
                log.warn("FastAPI 맞춤 피드 응답 본문이 비어있습니다: userId={}", userId);
                throw new CustomException(ErrorCode.NLP_9808);
            }

            log.info("FastAPI 맞춤 피드 수신 완료: {}개 기사", response.getArticles().size());
            return response;

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("FastAPI에서 사용자 프로필을 찾을 수 없음: userId={}", userId);
            return new FeedResponseDto(Collections.emptyList());
        } catch (ResourceAccessException e) {
            log.error("FastAPI 연결 시간 초과: {}", uri, e);
            throw new CustomException(ErrorCode.SERVER_5102);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("FastAPI HTTP 오류: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());

            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new CustomException(ErrorCode.FEED_9504); // 맞춤 피드의 기사를 찾을 수 없습니다
            }

            throw new CustomException(ErrorCode.NLP_9899); // NLP 내부 서버 오류

        } catch (Exception e) {
            log.error("FastAPI 맞춤 피드 요청 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.SERVER_5101);
        }
    }
}