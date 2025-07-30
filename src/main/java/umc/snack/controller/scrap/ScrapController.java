package umc.snack.controller.scrap;

import io.swagger.v3.oas.annotations.Operation;
import umc.snack.common.config.security.jwt.JWTUtil;
import umc.snack.domain.user.dto.UserScrapDto;
import umc.snack.domain.user.entity.UserScrap;
import umc.snack.service.scrap.UserScrapService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import umc.snack.common.dto.ApiResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/scraps")
@Tag(name = "Scrap", description = "스크랩 API")
public class ScrapController {

    private final UserScrapService userScrapService;
    private final JWTUtil jwtUtil;

    // 스크랩 추가
    @PostMapping("/{article_id}")
    @Operation(summary = "스크랩 추가")
    public ResponseEntity<?> addScrap(@PathVariable Long article_id, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserId(token);

        userScrapService.addScrap(userId, article_id);
        return ResponseEntity.ok(
                ApiResponse.onSuccess("SCRAP_6001", "스크랩 추가 성공")
        );
    }

    // 스크랩 취소
    @DeleteMapping("/{article_id}")
    @Operation(summary = "스크랩 취소")
    public ResponseEntity<?> cancelScrap(@PathVariable Long article_id, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserId(token);

        userScrapService.cancelScrap(userId, article_id);
        return ResponseEntity.ok(
                ApiResponse.onSuccess("SCRAP_6002", "스크랩 취소 성공")
        );
    }

    // 스크랩 목록 조회 (페이지네이션 처리)
    @GetMapping
    @Operation(summary = "스크랩 목록 조회 (페이지네이션 처리)")
    public ResponseEntity<?> getScrapList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserId(token);

        Page<UserScrap> scrapPage = userScrapService.getScrapList(userId, PageRequest.of(page, size));

        List<UserScrapDto> dtoList = scrapPage.getContent().stream()
                .map(UserScrapDto::from)
                .toList();

        Map<String, Object> responseData = Map.of(
                "scraps", dtoList,
                "totalPages", scrapPage.getTotalPages(),
                "totalElements", scrapPage.getTotalElements(),
                "currentPage", scrapPage.getNumber(),
                "hasNext", scrapPage.hasNext()
        );

        return ResponseEntity.ok(
                ApiResponse.onSuccess("SCRAP_6003", "스크랩 목록 조회 성공", responseData)
        );
    }

    // 스크랩 여부 확인
    @Operation(summary = "스크랩 여부 확인")
    @GetMapping("/{article_id}/exists")
    public ResponseEntity<?> checkScrapExists(@PathVariable Long article_id, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserId(token);

        boolean exists = userScrapService.isScrapped(userId, article_id);
        return ResponseEntity.ok(
                ApiResponse.onSuccess("SCRAP_6004", "스크랩 여부 확인 성공", Map.of("scrapped", exists))
        );
    }

    // 특정 스크랩 ID를 통해 해당 기사 상세 페이지로 리다이렉트
    @GetMapping("/{scrap_id}/redirect")
    @Operation(summary = "특정 스크랩 → 기사 리다이렉트")
    public ResponseEntity<Void> redirectToArticle(
            @PathVariable Long scrap_id,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserId(token);

        String articleUrl = userScrapService.getArticleUrlByScrapIdAndUserId(scrap_id, userId);
        return ResponseEntity
                .status(302) // HTTP 302 Found
                .header("Location", articleUrl)
                .build();
    }
}