package umc.snack.controller.article;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
@Tag(name = "Article", description = "기사 관련 API")
public class ArticleController {

    @Operation(summary = "기사 크롤링 상태 확인", description = "현재 크롤링 작업이 진행 중인지 확인합니다.")
    @GetMapping("/crawl/status")
    public ResponseEntity<?> checkCrawlStatus() {
        // TODO: 개발 예정
        return ResponseEntity.ok("크롤링 상태 확인 API - 개발 예정");
    }

    @Operation(summary = "기사 요약 생성", description = "AI 모델을 통해 기사 요약을 생성합니다.")
    @PostMapping("/{articleId}/summarize")
    public ResponseEntity<?> summarizeArticle(@PathVariable Long articleId) {
        // TODO: 개발 예정
        return ResponseEntity.ok("기사 요약 생성 API - 개발 예정");
    }

    @Operation(summary = "기사 단건 조회", description = "기사 ID로 기사 전체 내용을 조회합니다.")
    @GetMapping("/{articleId}")
    public ResponseEntity<?> getArticle(@PathVariable Long articleId) {
        // TODO: 개발 예정
        return ResponseEntity.ok("기사 단건 조회 API - 개발 예정");
    }

    @Operation(summary = "주요 용어 추출", description = "기사를 분석하여 주요 용어를 추출합니다.")
    @PostMapping("/{articleId}/terms/extract")
    public ResponseEntity<?> extractTerms(@PathVariable Long articleId) {
        // TODO: 개발 예정
        return ResponseEntity.ok("주요 용어 추출 API - 개발 예정");
    }

    @Operation(summary = "주요 용어 조회", description = "기사에 대해 추출된 용어들을 조회합니다.")
    @GetMapping("/{articleId}/terms")
    public ResponseEntity<?> getTerms(@PathVariable Long articleId) {
        // TODO: 개발 예정
        return ResponseEntity.ok("주요 용어 조회 API - 개발 예정");
    }
}