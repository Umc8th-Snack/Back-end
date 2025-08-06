package umc.snack.controller.memo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import umc.snack.common.config.security.CustomUserDetails;
import umc.snack.common.dto.ApiResponse;
import umc.snack.domain.memo.dto.MemoResponseDto;
import umc.snack.service.memo.MemoCommandService;

@RestController
@RequestMapping("/api/memos")
@RequiredArgsConstructor
@Tag(name = "Memo Redirect", description = "메모 리다이렉트 관련 API")
public class MemoRedirectController {
    
    private final MemoCommandService memoCommandService;

    @Operation(summary = "특정 메모의 기사로 리다이렉트", description = "마이 페이지에서 특정 메모를 클릭하면 해당 기사로 리다이렉션하는 API입니다.")
    @GetMapping("/{memo_id}/redirect")
    @ResponseStatus(HttpStatus.FOUND)
    public ApiResponse<String> redirectToArticle(@PathVariable("memo_id") Long memoId, Long userId,
                                                 @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userID = customUserDetails.getUserId();

        MemoResponseDto.RedirectResultDto resultDto = memoCommandService.redirectToArticle(memoId, userId);
        
        return ApiResponse.onSuccess(
                "MEMO_8501",
                "해당 메모장의 기사 리다이렉트 성공",
                resultDto.getRedirectUrl()
        );
    }
} 