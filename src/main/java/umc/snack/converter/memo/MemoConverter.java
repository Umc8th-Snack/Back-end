package umc.snack.converter.memo;

import umc.snack.domain.memo.dto.MemoResponseDto;
import umc.snack.domain.memo.entity.Memo;

public class MemoConverter {
    public static MemoResponseDto.CreateResultDto toCreateResultDto(Memo memo) {
        return MemoResponseDto.CreateResultDto.builder()
                .memoId(memo.getMemoId())
                .content(memo.getContent())
                .build();
    }

    public static MemoResponseDto.UpdateResultDto toUpdateResultDto(Memo memo) {
        return MemoResponseDto.UpdateResultDto.builder()
                .memoId(memo.getMemoId())
                .content(memo.getContent())
                .build();
    }
}

