package umc.snack.controller.memo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import umc.snack.common.config.security.CustomUserDetails;
import umc.snack.common.dto.ApiResponse;
import umc.snack.domain.memo.dto.MemoResponseDto;
import umc.snack.service.memo.MemoQueryService;
@RestController
@RequestMapping("/api/memos")
@RequiredArgsConstructor
@Tag(name = "Memo", description = "기사 메모 관련 API")
public class MyMemoController {
    private final MemoQueryService memoQueryService;

    @Operation(summary = "내가 작성한 메모 목록 조회")
    @GetMapping("")
    public ApiResponse<MemoResponseDto.MemoListDto> getMyMemos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        Long userId = customUserDetails.getUserId();
        MemoResponseDto.MemoListDto resultDto = memoQueryService.getMemosByUser(userId, page, size);
        return ApiResponse.onSuccess("MEMO_8504", "내 메모 목록을 성공적으로 조회했습니다.", resultDto);
    }
}