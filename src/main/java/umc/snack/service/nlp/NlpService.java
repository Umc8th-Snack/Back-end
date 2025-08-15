package umc.snack.service.nlp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import jakarta.annotation.PostConstruct;
import org.springframework.web.util.UriComponentsBuilder;
import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;
import umc.snack.domain.nlp.dto.FeedResponseDto;
import umc.snack.domain.nlp.dto.SearchResponseDto;
import umc.snack.domain.nlp.dto.UserInteractionDto;
import umc.snack.domain.nlp.dto.UserProfileRequestDto;

import java.net.URI;
import java.util.*;

@Slf4j
@Service
public class NlpService {

    private final RestTemplate fastApiRestTemplate;
    private final RestTemplate longTimeoutRestTemplate;
    private final String fastapiUrl;

    // application.yml 파일에서 fastapi.url 가져오기!!
    public NlpService(@Qualifier("fastApiRestTemplate") RestTemplate fastApiRestTemplate,
                      @Qualifier("longTimeoutRestTemplate") RestTemplate longTimeoutRestTemplate,
                      @Value("${fastapi.url}") String fastapiUrl) {
        this.fastApiRestTemplate = fastApiRestTemplate;
        this.longTimeoutRestTemplate = longTimeoutRestTemplate;
        this.fastapiUrl = fastapiUrl;
    }

    @PostConstruct
    public void initialize() {
        log.info("NLP 서비스 초기화 - FastAPI URL: {}", fastapiUrl);
        checkFastApiHealth();
    }

    /**
     * FastAPI 헬스체크
     */
    public boolean checkFastApiHealth() {
        try {
            String healthUrl = fastapiUrl + "/health";
            ResponseEntity<Map> response = fastApiRestTemplate.getForEntity(healthUrl, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("FastAPI 서버 연결 확인: {}", fastapiUrl);
                return true;
            }
            log.warn("FastAPI 헬스체크 실패. 상태 코드: {}", response.getStatusCode());
            return false;

        } catch (ResourceAccessException e) {
            throw new CustomException(ErrorCode.SERVER_5102);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.SERVER_5101);
        }
    }

    /**
     * 전체 기사 벡터화 - FastAPI의 실제 엔드포인트 호출
     */
    public Map<String, Object> processAllArticles(boolean reprocess) {
        log.info("전체 기사 처리 요청 - 재처리: {}", reprocess);
        String url = fastapiUrl + "/api/nlp/vectorize/batch";
        HttpEntity<Void> request = new HttpEntity<>(new HttpHeaders());

        try {
            ResponseEntity<Map> response = longTimeoutRestTemplate.postForEntity(url, request, Map.class);
            if (response.getBody() != null) {
                log.info("FastAPI 응답: {}", response.getBody());
                return response.getBody();
            }
            // 응답 본문이 null인 경우
            throw new CustomException(ErrorCode.NLP_9899);
        } catch (ResourceAccessException e) {
            log.error("FastAPI 연결 시간 초과: {}", url, e);
            throw new CustomException(ErrorCode.SERVER_5102);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("FastAPI HTTP 오류: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomException(ErrorCode.NLP_9899);
        } catch (Exception e) {
            log.error("FastAPI 호출 중 알 수 없는 오류: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.SERVER_5101);
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
            // 명세서에 따라 더 구체적인 오류 매핑 가능
            throw new CustomException(ErrorCode.NLP_9806);
        } catch (Exception e) {
            log.error("벡터화 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.SERVER_5101);
        }
    }

    /**
     * 의미 기반 검색 (수정된 부분)
     * @return SearchResponseDto
     */
    public SearchResponseDto searchArticles(String query, int page, int size, double threshold) {
        log.info("기사 검색 요청 - 검색어: '{}', 페이지: {}, 크기: {}", query, page, size);

        URI uri = UriComponentsBuilder.fromHttpUrl(fastapiUrl)
                .path("/api/articles/search")
                .queryParam("query", query)
                .queryParam("page", page)
                .queryParam("size", size)
                .queryParam("threshold", threshold)
                .build(true).toUri();

        try {
            ResponseEntity<SearchResponseDto> response = fastApiRestTemplate.getForEntity(uri, SearchResponseDto.class);
            if (response.getBody() == null) {
                throw new CustomException(ErrorCode.NLP_9899);
            }
            return response.getBody();
        } catch (ResourceAccessException e) {
            log.error("FastAPI 연결 시간 초과: {}", uri, e);
            throw new CustomException(ErrorCode.SERVER_5102);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("FastAPI 검색 HTTP 오류: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomException(ErrorCode.NLP_9899);
        } catch (Exception e) {
            log.error("FastAPI 검색 호출 중 알 수 없는 오류: {}", e.getMessage(), e);
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

    /**
     * 맞춤 피드 기사 ID 목록 요청
    */
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
                throw new CustomException(ErrorCode.NLP_9899);
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
            log.error("FastAPI 맞춤 피드 HTTP 오류: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomException(ErrorCode.NLP_9899);
        } catch (Exception e) {
            log.error("FastAPI 맞춤 피드 요청 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.SERVER_5101);
        }
    }
}
