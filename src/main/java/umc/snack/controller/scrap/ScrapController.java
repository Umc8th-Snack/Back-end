package umc.snack.controller.scrap;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/scraps")
@Tag(name = "Scrap", description = "스크랩 API")
public class ScrapController {

    // 스크랩 추가
    @PostMapping("/{article_id}")
    public ResponseEntity<?> addScrap(@PathVariable Long article_id) {
        // TODO: 스크랩 추가 로직 구현
        return ResponseEntity.ok("스크랩 추가 완료");
    }

    // 스크랩 취소
    @DeleteMapping("/{article_id}")
    public ResponseEntity<?> cancelScrap(@PathVariable Long article_id) {
        // TODO: 스크랩 취소 로직 구현
        return ResponseEntity.ok("스크랩 취소 완료");
    }

    // 스크랩 목록 조회 (페이징 처리)
    @GetMapping
    public ResponseEntity<?> getScrapList(@RequestParam int page, @RequestParam int size) {
        // TODO: 스크랩 목록 조회 로직 구현
        return ResponseEntity.ok("스크랩 목록 조회");
    }

    // 스크랩 여부 확인
    @GetMapping("/{article_id}/exists")
    public ResponseEntity<?> checkScrapExists(@PathVariable Long article_id) {
        // TODO: 스크랩 여부 확인 로직 구현
        return ResponseEntity.ok("스크랩 여부 확인됨");
    }

    // 특정 스크랩 → 기사 리다이렉트
    @GetMapping("/{scrap_id}/redirect")
    public ResponseEntity<?> redirectToArticle(@PathVariable Long scrap_id) {
        // TODO: 기사 URL로 리다이렉트 처리
        return ResponseEntity.ok("기사 리다이렉트");
    }
}
