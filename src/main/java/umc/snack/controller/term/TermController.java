package umc.snack.controller.term;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import umc.snack.common.dto.ApiResponse;
import umc.snack.domain.term.dto.TermResponseDto;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Term", description = "용어 관련 API")
public class TermController {

    // TODO: 서비스 계층 주입 활성화
    // private final TermService termService;

    @Operation(summary = "전체 용어 사전 조회", description = "등록된 모든 용어와 해설을 조회합니다. (개발/관리자용)")
    @GetMapping("/terms")
    public ResponseEntity<ApiResponse<List<TermResponseDto>>> getAllTerms() {
        // TODO: 서비스 계층 메서드 호출로 대체
        // List<TermResponseDto> terms = termService.getAllTerms();

        // 현재는 서비스 계층이 없으므로 임시 데이터를 만듭니다.
        List<TermResponseDto> terms = Arrays.asList(
                new TermResponseDto(
                        "말",
                        Arrays.asList("사람이 의사를 표현하기 위해 사용하는 언어", "네 다리 달린 동물로, 빠르게 달릴 수 있음"),
                        LocalDateTime.of(2025, 7, 1, 12, 0, 0)
                ),
                new TermResponseDto(
                        "눈",
                        Arrays.asList("얼음 결정이 하늘에서 떨어지는 현상", "사람의 시각 기관"),
                        LocalDateTime.of(2025, 7, 1, 12, 3, 0)
                )
        );


        // 조회값이 null이면 서비스에서 NoTermsFoundException을 던지도록 구현

        return ResponseEntity.ok(
                ApiResponse.onSuccess("TERM_7001", "전체 용어 조회에 성공했습니다.", terms)
        );
    }
}