package umc.snack.controller.share;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Share", description = "공유 관련 API")
public class ShareController {

    @Operation(summary = "공유 URL 생성", description = "특정 기사의 공유용 UUID를 생성합니다. 버튼 누르면 바로 복사되도록.")
    @PostMapping("/articles/{articleId}/share")
    public ResponseEntity<?> createShare(@PathVariable Long articleId) {
        // TODO: 공유 UUID 생성 로직 추가 예정
        return ResponseEntity.ok("공유 URL 생성 API - 개발 예정");
    }

    @Operation(summary = "공유 기사 조회", description = "UUID 기반으로 공유된 기사 내용을 조회합니다.")
    @GetMapping("/share/{uuid}")
    public ResponseEntity<?> getSharedArticle(@PathVariable String uuid) {
        // TODO: 공유된 기사 조회 로직 추가 예정
        return ResponseEntity.ok("공유 기사 조회 API - 개발 예정");
    }
}