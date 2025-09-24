package umc.snack.service.memo;

import umc.snack.domain.memo.dto.MemoResponseDto;

public interface MemoQueryService {
    MemoResponseDto.MemoListDto getMemosByUser(Long userId, int page, int size);

    MemoResponseDto.ContentDto getMyMemoForArticle(Long articleId, Long userId);
}
