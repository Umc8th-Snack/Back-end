package umc.snack.service.memo;

import umc.snack.domain.memo.dto.MemoRequestDto;
import umc.snack.domain.memo.entity.Memo;

public interface MemoCommandService {
    MemoResponseDto.CreateResultDto createMemo(Long articleId, MemoRequestDto.CreateDto request);
}
