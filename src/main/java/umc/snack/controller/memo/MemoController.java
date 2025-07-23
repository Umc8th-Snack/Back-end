package umc.snack.controller.memo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import umc.snack.common.dto.ApiResponse;
import umc.snack.domain.memo.dto.MemoRequestDto;
import umc.snack.domain.memo.dto.MemoResponseDto;
import umc.snack.service.memo.MemoCommandService;

@RestController
@RequestMapping("/api/articles/{article_id}/memos")
@RequiredArgsConstructor
@Tag(name = "Memo", description = "기사 메모 관련 API")
public class MemoController {
    private final MemoCommandService memoCommandService;
    @Operation(summary = "특정 기사에 메모 작성", description = "현재 보고 있는 기사에 대한 메모를 작성하는 API입니다.")
    @PostMapping
    public ApiResponse<MemoResponseDto.CreateResultDto> createMemo (
            @PathVariable("article_id") Long article_id,
            @RequestBody @Valid MemoRequestDto.CreateDto request) {


        MemoResponseDto.CreateResultDto resultDto = memoCommandService.createMemo(article_id, request);
        return ApiResponse.onSuccess(
                "MEMO_8502",
                "메모가 성공적으로 생성되었습니다.",
                resultDto
        );
    }

    @Operation(summary = "특정 기사의 메모 수정", description = "특정 기사에 작성된 메모를 수정하는 API입니다.")
    @PatchMapping("/{memo_id}")
    public ApiResponse<MemoResponseDto.UpdateResultDto> updateMemo(
            @PathVariable("article_id") Long article_id,
            @PathVariable("memo_id") Long memo_id,
            @RequestBody @Valid MemoRequestDto.UpdateDto request) {

        MemoResponseDto.UpdateResultDto resultDto = memoCommandService.updateMemo(article_id, memo_id, request);

        return ApiResponse.onSuccess(
                "MEMO_8503",
                "메모가 성공적으로 수정되었습니다.",
                resultDto
        );
    }

    @Operation(summary = "특정 기사의 메모 삭제", description = "특정 기사에 작성된 메모를 삭제하는 API입니다.")
    @DeleteMapping("/{memo_id}")
    public ApiResponse<Object> deleteMemo(
            @PathVariable("article_id") Long article_id,
            @PathVariable("memo_id") Long memo_id) {

        memoCommandService.deleteMemo(article_id, memo_id);

        return ApiResponse.onSuccess(
                "MEMO_8501",
                "메모가 성공적으로 삭제되었습니다.",
                null
        );
    }


}