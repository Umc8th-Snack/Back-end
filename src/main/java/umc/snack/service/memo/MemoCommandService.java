package umc.snack.service.memo;

import umc.snack.domain.memo.dto.MemoRequestDto;
import umc.snack.domain.memo.dto.MemoResponseDto;
import umc.snack.domain.memo.entity.Memo;

public interface MemoCommandService {
    MemoResponseDto.CreateResultDto createMemo(Long articleId, MemoRequestDto.CreateDto request, Long userId);

    MemoResponseDto.UpdateResultDto updateMemo(Long articleId, Long memoId, MemoRequestDto.UpdateDto request, Long userId);

    void deleteMemo(Long articleId, Long memoID, Long userId);

    MemoResponseDto.RedirectResultDto redirectToArticle(Long memoId, Long userId);
}
