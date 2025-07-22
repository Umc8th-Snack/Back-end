package umc.snack.service.memo;

import ch.qos.logback.core.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.memo.dto.MemoRequestDto;
import umc.snack.domain.memo.entity.Memo;
import umc.snack.domain.user.entity.User;
import umc.snack.repository.article.ArticleRepository;
import umc.snack.repository.memo.MemoRepository;
import umc.snack.repository.user.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class MemoCommandServiceImpl implements MemoCommandService {
    private final MemoRepository memoRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    @Override
    public Memo createMemo(Long articleId, MemoRequestDto.CreateDto request) {
        User currentUser = userRepository.findById(1L)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_2622));
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMO_8605));
        Memo newMemo = Memo.builder()
                .content(request.getContent())
                .article(article)
                .build();

        return memoRepository.save(newMemo);
    }
}
