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

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Share", description = "공유 관련 API")
public class ShareController {

    // TODO: 서비스 계층은 나중에 주입. 지금은 컨트롤러 정의만.
    // private final ShareService shareService;


    @Operation(summary = "공유 URL 생성", description = "특정 기사의 공유용 UUID를 생성합니다. 버튼 누르면 바로 복사되도록.")
    @PostMapping("/articles/{articleId}/share")
    public ResponseEntity<ApiResponse<ShareResultDto>> createShare(@PathVariable Long articleId) {
        // TODO: 공유 UUID 생성 로직 추가 예정
        // ShareResultDto result = shareService.createShareLink(articleId, /* userId (from token) */);

        // 현재는 서비스 계층이 없으므로 임시 데이터를 만들었어요
        // shareUuid 컬럼 값은 이 uuid에 해당합니다.
        String tempUuid = java.util.UUID.randomUUID().toString();
        String tempSharedUrl = "https://snack.com/share/" + tempUuid;
        ShareResultDto result = new ShareResultDto(tempUuid, tempSharedUrl);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.onSuccess("SHARE_6006", "공유 링크 생성 성공", result)
        );
    }


    @Operation(summary = "공유 기사 조회", description = "UUID 기반으로 공유된 기사 내용을 조회합니다.")
    @GetMapping("/share/{uuid}")
    public ResponseEntity<ApiResponse<SharedArticleContentDto>> getSharedArticle(@PathVariable String uuid) {
        // TODO: 공유된 기사 조회 로직 추가 예정
        // SharedArticleContentDto sharedArticleContent = shareService.getSharedArticleByUuid(uuid);

        // 현재는 서비스 계층이 없으므로 임시 데이터를 만들었어요.
        SharedArticleContentDto sharedArticleContent = new SharedArticleContentDto(
                123L, "샘플 기사 제목", "이것은 샘플 기사의 요약입니다.",
                java.time.LocalDateTime.of(2024, 7, 10, 15, 30, 0),
                "https://original.article.com/example", "기술"
        );

        // 서비스에서 데이터를 찾지 못하면 null을 반환할 것이고, 이때 전역 예외 처리기에서 404를 처리해야 합니다.

        return ResponseEntity.ok(
                ApiResponse.onSuccess("SHARE_6007", "공유 기사 조회에 성공했습니다.", sharedArticleContent)
        );
    }
}