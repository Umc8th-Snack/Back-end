package umc.snack.converter.memo;

import org.springframework.data.domain.Page;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.memo.dto.MemoResponseDto;
import umc.snack.domain.memo.entity.Memo;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    public static MemoResponseDto.MemoInfo toMemoInfo(Memo memo) {

        Long articleId = Optional.ofNullable(memo.getArticle())
                .map(Article::getArticleId)
                .orElse(null); // 기사가 없는 메모는 articleId를 null로 설정

        return MemoResponseDto.MemoInfo.builder()
                .memoId(memo.getMemoId())
                .content(memo.getContent())
                .createdAt(memo.getCreatedAt())
                // .articleUrl(memo.getArticle().getArticleUrl())
                .articleId(articleId)
                .build();
    }

    public static MemoResponseDto.MemoListDto toMemoListDto(Page<Memo> memoPage) {
        List<MemoResponseDto.MemoInfo> memoInfos = memoPage.getContent().stream()
                .map(MemoConverter::toMemoInfo)
                .collect(Collectors.toList());

        return MemoResponseDto.MemoListDto.builder()
                .memos(memoInfos)
                .page(memoPage.getNumber())
                .size(memoPage.getSize())
                .totalPages(memoPage.getTotalPages())
                .totalElements(memoPage.getTotalElements())
                .build();
    }
}

