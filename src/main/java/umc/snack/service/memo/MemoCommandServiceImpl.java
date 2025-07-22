package umc.snack.service.memo;

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
        // JWT 인증 토큰을 발급하는 시스템이 아직 구현되어있지 않아서 구현한 임시코드입니다.
        User currentUser = userRepository.findById(1L)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_2622));
        /* 인증 시스템 구현 완료 되면 아래 코드로 수정 예정입니다.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = ((UserPrincipal) authentication.getPrincipal()).getId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_2622));
        */
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMO_8605));
        Memo newMemo = Memo.builder()
                .content(request.getContent())
                .article(article)
                .user(currentUser)
                .build();

        return memoRepository.save(newMemo);
    }
}
