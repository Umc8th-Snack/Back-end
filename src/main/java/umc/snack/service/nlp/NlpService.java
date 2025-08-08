package umc.snack.service.nlp;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import umc.snack.domain.article.entity.Article;
import umc.snack.domain.nlp.dto.*;
import umc.snack.domain.article.entity.ArticleSemanticVector; // 기사 벡터 저장 엔티티
import umc.snack.repository.article.ArticleRepository;
import umc.snack.repository.nlp.ArticleSemanticVectorRepository;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NlpService {

    private final ArticleSemanticVectorRepository articleSemanticVectorRepository;
    private final ArticleRepository articleRepository;
    private final RestTemplate restTemplate;

    @Value("${fastapi.url}")
    private String fastapiUrl;

    /**
     * FastAPI를 통해 기사 본문을 벡터화하고, 결과를 DB에 저장합니다.
     */
    public ArticleVectorizeListResponseDto processAndSaveArticleVectors(List<ArticleVectorizeRequestDto> articleRequestDtos) {
        // 1. FastAPI로 기사 본문 전송
        String url = fastapiUrl + "/vectorize/articles";
        ArticleVectorizeListResponseDto response = restTemplate.postForObject(url, articleRequestDtos, ArticleVectorizeListResponseDto.class);

        // 2. 응답받은 벡터와 키워드를 DB에 저장
        if (response != null && response.getResults() != null) {
            List<ArticleSemanticVector> vectorsToSave = response.getResults().stream()
                    .map(resultDto -> {
                        // 기사 ID로 Article 엔티티를 먼저 조회합니다. (패키지명 수정: umc.snack.domain.article.entity.Article)
                        Article article = articleRepository.findById(resultDto.getArticleId())
                                .orElseThrow(() -> new IllegalArgumentException("Article not found with id: " + resultDto.getArticleId()));

                        ArticleSemanticVector vector = ArticleSemanticVector.builder()
                                .article(article)
                                .keywords(resultDto.getKeywords().stream()
                                        .map(kw -> kw.getWord() + ":" + kw.getTfidf())
                                        .collect(java.util.stream.Collectors.joining(",")))
                                .build();

                        // DTO의 double[]을 엔티티의 setVector() 메서드를 사용해 String으로 변환하여 저장
                        vector.setVector(resultDto.getVector());
                        return vector;
                    })
                    .collect(java.util.stream.Collectors.toList());

            articleSemanticVectorRepository.saveAll(vectorsToSave);
        }

        return response;
    }

    /**
     * FastAPI를 통해 검색 쿼리를 벡터화합니다. (내부 호출용)
     */
    public QueryVectorizeResponseDto processQueryVector(String query) {
        String url = fastapiUrl + "/vectorize/query";
        QueryVectorizeRequestDto requestDto = new QueryVectorizeRequestDto();
        requestDto.setQuery(query);
        return restTemplate.postForObject(url, requestDto, QueryVectorizeResponseDto.class);
    }

    /**
     * 최종 사용자 검색 API: 검색 쿼리 벡터화 -> DB 기사 벡터와 유사도 계산 -> 정렬 후 반환
     */
    public SearchArticleResponseDto searchArticles(String query, int page, int size) {
        // 1. FastAPI를 호출하여 검색 쿼리 벡터화
        QueryVectorizeResponseDto queryVectorDto = processQueryVector(query);
        double[] queryVector = queryVectorDto.getVector();

        // 2. DB에서 모든 기사 의미 벡터 불러오기
        List<ArticleSemanticVector> allVectors = articleSemanticVectorRepository.findAll();

        // 3. 코사인 유사도 계산 및 정렬
        List<ArticleSimilarityResultDto> similarityResults = allVectors.stream()
                .map(articleVector -> {
                    double similarity = calculateCosineSimilarity(queryVector, articleVector.getVectorArray());
                    return new ArticleSimilarityResultDto(articleVector.getArticle().getArticleId(), similarity);
                })
                .sorted(Comparator.comparingDouble(ArticleSimilarityResultDto::getSimilarity).reversed())
                .collect(Collectors.toList());

        // 4. 페이징 처리 및 기사 상세 정보 조회
        int start = page * size;
        int end = Math.min(start + size, similarityResults.size());

        List<Long> articleIds = similarityResults.subList(start, end).stream()
                .map(ArticleSimilarityResultDto::getArticleId)
                .collect(Collectors.toList());

        List<Article> articles = articleRepository.findAllById(articleIds);

        // 5. 응답 DTO로 변환
        return new SearchArticleResponseDto(articles);
    }

    // 코사인 유사도 계산 메서드
    private double calculateCosineSimilarity(double[] vectorA, double[] vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}