package umc.snack.controller.scrap;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.PostConstruct;
import umc.snack.Mock.UserMock;
import umc.snack.repository.user.UserRepository;
import umc.snack.domain.user.entity.User;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import umc.snack.domain.user.dto.UserScrapDto;
import umc.snack.domain.user.entity.UserScrap;
import umc.snack.service.scrap.UserScrapService;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/scraps")
@Tag(name = "Scrap", description = "스크랩 API")
public class ScrapController {

    private final UserScrapService userScrapService;
    private final UserRepository userRepository;
    private Long mockUserId;

    @PostConstruct
    public void initMockUser() {
        String mockEmail = "mockuser@example.com";
        User existingUser = userRepository.findByEmail(mockEmail).orElse(null);

        if (existingUser != null) {
            this.mockUserId = existingUser.getUserId();
        } else {
            User mockUser = UserMock.mock();
            User savedUser = userRepository.save(mockUser);
            this.mockUserId = savedUser.getUserId();
        }
    }

    // 스크랩 추가
    @PostMapping("/{article_id}")
    @Operation(summary = "스크랩 추가")
    public ResponseEntity<?> addScrap(@PathVariable Long article_id) {
        Long userId = mockUserId;

        // TODO: JWT 인증 적용 시 실제 로그인한 유저 정보로 대체할 예정
        /*
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Long userId = userService.getUserIdByEmail(email); // 또는 커스텀 UserDetails에서 userId 꺼내기
        */

        userScrapService.addScrap(userId, article_id);
        return ResponseEntity.ok(
                Map.of(
                        "isSuccess", true,
                        "code", "SCRAP_6001",
                        "message", "스크랩 추가 완료"
                )
        );
    }

    // 스크랩 취소
    @DeleteMapping("/{article_id}")
    @Operation(summary = "스크랩 취소")
    public ResponseEntity<?> cancelScrap(@PathVariable Long article_id) {
        Long userId = mockUserId;

        userScrapService.cancelScrap(userId, article_id);
        return ResponseEntity.ok(
                Map.of(
                        "isSuccess", true,
                        "code", "SCRAP_6002",
                        "message", "스크랩 취소 완료"
                )
        );
    }

    // 스크랩 목록 조회 (페이지네이션 처리)
    @GetMapping
    @Operation(summary = "스크랩 목록 조회 (페이지네이션 처리)")
    public ResponseEntity<?> getScrapList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = mockUserId;

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
                Map.of(
                        "isSuccess", true,
                        "code", "SCRAP_6003",
                        "message", "스크랩 목록 조회 성공",
                        "result", responseData
                )
        );
    }

    // 스크랩 여부 확인
    @Operation(summary = "스크랩 여부 확인")
    @GetMapping("/{article_id}/exists")
    public ResponseEntity<?> checkScrapExists(@PathVariable Long article_id) {
        Long userId = mockUserId;

        boolean exists = userScrapService.isScrapped(userId, article_id);
        return ResponseEntity.ok(
                Map.of(
                        "isSuccess", true,
                        "code", "SCRAP_6004",
                        "message", "스크랩 여부 확인 성공",
                        "result", Map.of("scrapped", exists)
                )
        );
    }

    // 특정 스크랩 ID를 통해 해당 기사 상세 페이지로 리다이렉트
    @GetMapping("/{scrap_id}/redirect")
    @Operation(summary = "특정 스크랩 → 기사 리다이렉트")
    public ResponseEntity<Void> redirectToArticle(@PathVariable Long scrap_id) {
        String articleUrl = userScrapService.getArticleUrlByScrapId(scrap_id);
        return ResponseEntity
                .status(302) // HTTP 302 Found
                .header("Location", articleUrl)
                .build();
    }
}