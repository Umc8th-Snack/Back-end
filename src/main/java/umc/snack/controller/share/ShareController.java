package umc.snack.controller.share;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import umc.snack.common.dto.ApiResponse; // 공통 응답 DTO 임포트
import umc.snack.domain.share.dto.ShareResultDto; // POST 응답 result용 DTO 임포트 (새로 정의)
import umc.snack.domain.share.dto.SharedArticleContentDto; // GET 응답 result용 DTO 임포트 (새로 정의)
import umc.snack.service.share.ShareService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Share", description = "공유 관련 API")
public class ShareController {

    private final ShareService shareService;

    @Operation(
            summary = "공유 URL 생성",
            description = "특정 기사의 공유용 UUID를 생성합니다. 버튼 누르면 바로 복사되도록."
    )
    @PostMapping("/articles/{articleId}/share")
    public ResponseEntity<ApiResponse<ShareResultDto>> createShare(@PathVariable Long articleId) {
        // TODO: 추후 인증 연동 시 userId → SecurityContext 등에서 가져와서 전달
        ShareResultDto result = shareService.createShareLink(articleId, null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.onSuccess("SHARE_6501", "공유 링크 생성 성공", result));
    }

    @Operation(
            summary = "공유 기사 조회",
            description = "UUID 기반으로 공유된 기사 내용을 조회합니다."
    )
    @GetMapping("/share/{uuid}")
    public ResponseEntity<ApiResponse<SharedArticleContentDto>> getSharedArticle(@PathVariable String uuid) {
        SharedArticleContentDto result = shareService.getSharedArticleByUuid(uuid);
        return ResponseEntity.ok(
                ApiResponse.onSuccess("SHARE_6502", "공유 기사 조회에 성공했습니다.", result)
        );
    }
}