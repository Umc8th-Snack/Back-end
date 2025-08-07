package umc.snack.service.memo;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;
import umc.snack.converter.memo.MemoConverter;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.memo.dto.MemoResponseDto;
import umc.snack.domain.memo.entity.Memo;
import umc.snack.domain.user.entity.User;
import umc.snack.repository.article.ArticleRepository;
import umc.snack.repository.memo.MemoRepository;
import umc.snack.repository.user.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemoQueryServiceImpl implements MemoQueryService {
    private final MemoRepository memoRepository;
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;

    @Override
    public MemoResponseDto.MemoListDto getMemosByUser(Long userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_2622));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Memo> memoPage = memoRepository.findByUser(user, pageable);

        if (memoPage.isEmpty() && page > 0)
            throw new CustomException(ErrorCode.REQ_3104);

        return MemoConverter.toMemoListDto(memoPage);
    }

    @Override
    public MemoResponseDto.ContentDto getMyMemoForArticle(Long articleId, Long userId) {
        // 유효한 사용자인지 확인
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_2622));

        // 유효한 기사인지 확인
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMO_8605));

        // 해당 사용자가 해당 기사에 작성한 메모 조회
        Memo memo = memoRepository.findByUserAndArticle(currentUser, article)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMO_8601)); // 해당 기사에 작성된 메모를 찾을 수 없습니다.

        return MemoResponseDto.ContentDto.builder()
                .content(memo.getContent())
                .build();
    }
}
