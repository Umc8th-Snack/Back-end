package umc.snack.controller.Term;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Term", description = "용어 관련 API")
public class TermController {

    @Operation(summary = "전체 용어 사전 조회", description = "등록된 모든 용어와 해설을 조회합니다. (개발/관리자용)")
    @GetMapping("/terms")
    public ResponseEntity<?> getAllTerms() {
        // TODO: 전체 용어 조회 로직 추가 예정
        return ResponseEntity.ok("전체 용어 사전 조회 API - 개발 예정");
    }
}