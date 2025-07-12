package umc.snack.controller.memo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/articles/{article_id}/memos")
@RequiredArgsConstructor
@Tag(name = "Memo", description = "기사 메모 관련 API")
public class MemoController {
    @Operation(summary = "특정 기사에 메모 작성", description = "현재 보고 있는 기사에 대한 메모를 작성하는 API입니다.")
    @PostMapping
    public ResponseEntity<?> createMemo(
            @PathVariable Long article_id,
            @RequestBody Object NoteDto) {
        // TODO: 개발 예정
        return ResponseEntity.ok("메모 작성 API - 개발 예정 (articleId: " + article_id + ")");
    }

    @Operation(summary = "특정 기사의 메모 수정", description = "특정 기사에 작성된 메모를 수정하는 API입니다.")
    @PatchMapping("/{memo_id}")
    public ResponseEntity<?> updateMemo(
            @PathVariable Long article_id,
            @PathVariable Long memo_id,
            @RequestBody Object NoteDto) {
        // TODO: 개발 예정
        return ResponseEntity.ok("메모 수정 API - 개발 예정 (articleId: " + article_id + ", memoId: " + memo_id + ")");
    }

    @Operation(summary = "특정 기사의 메모 삭제", description = "특정 기사에 작성된 메모를 삭제하는 API입니다.")
    @DeleteMapping("/{memo_id}")
    public ResponseEntity<?> deleteMemo(
            @PathVariable Long article_id,
            @PathVariable Long memo_id) {
        // TODO: 개발 예정
        return ResponseEntity.ok("메모 삭제 API - 개발 예정 (articleId: " + article_id + ", memoId: " + memo_id + ")");
    }
}