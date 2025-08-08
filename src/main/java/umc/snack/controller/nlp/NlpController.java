package umc.snack.controller.nlp;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import umc.snack.common.dto.ApiResponse;
import umc.snack.domain.nlp.dto.*;
import umc.snack.service.nlp.NlpService;

@RestController
@RequestMapping("/api/nlp")
@RequiredArgsConstructor
@Tag(name = "NLP", description = "자연어 처리 및 검색 관련 API")
public class NlpController {

    private final NlpService nlpService;

    @Operation(
            summary = "기사 벡터화",
            description = "새로 수집된 기사들을 받아 본문을 분석하고 의미 벡터를 생성하여 DB에 저장합니다. (내부용)"
    )
    @PostMapping("/vectorize/articles")
    public ResponseEntity<ApiResponse<ArticleVectorizeListResponseDto>> vectorizeArticles(
            @RequestBody @Valid ArticleVectorizeListRequestDto requestDto) {

        // DTO에 정의된 List<ArticleVectorizeRequestDto>를 받아서 서비스로 전달
        ArticleVectorizeListResponseDto result = nlpService.processAndSaveArticleVectors(requestDto.getArticles());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.onSuccess("NLP_9700", "기사 벡터화에 성공했습니다.", result));
    }

    @Operation(
            summary = "검색 쿼리 벡터화",
            description = "사용자의 검색 쿼리를 받아 벡터화한 결과를 반환합니다. (내부용)"
    )
    @PostMapping("/vectorize/query")
    public ResponseEntity<ApiResponse<QueryVectorizeResponseDto>> vectorizeQuery(
            @RequestBody  @Valid QueryVectorizeRequestDto requestDto) {

        QueryVectorizeResponseDto result = nlpService.processQueryVector(requestDto.getQuery());

        return ResponseEntity.ok(
                ApiResponse.onSuccess("NLP_9701", "검색어 벡터화에 성공했습니다.", result)
        );
    }

    // 최종 사용자 검색을 위한 API
    @Operation(
            summary = "기사 검색",
            description = "사용자의 검색 쿼리에 대해 코사인 유사도 기반으로 관련 기사를 정렬하여 반환합니다."
    )
    @GetMapping("/search/articles")
    public ResponseEntity<ApiResponse<SearchArticleResponseDto>> searchArticles(
            @RequestParam(name = "query") @NotBlank String query,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "10") @Min(1) @Max(100) int size) {

        SearchArticleResponseDto result = nlpService.searchArticles(query, page, size);

        return ResponseEntity.ok(
                ApiResponse.onSuccess("NLP_5003", "기사 검색 성공", result)
        );
    }
}