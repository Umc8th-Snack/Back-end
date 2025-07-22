package umc.snack.service.memo;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;
import umc.snack.converter.memo.MemoConverter;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.memo.dto.MemoRequestDto;
import umc.snack.domain.memo.dto.MemoResponseDto;
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
    public MemoResponseDto.CreateResultDto createMemo(Long articleId, MemoRequestDto.CreateDto request) {
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

        Memo savedMemo = memoRepository.save(newMemo);

        return MemoConverter.toCreateResultDto(savedMemo);
    }

    @Override
    public MemoResponseDto.UpdateResultDto updateMemo(Long articleId, Long memoId, MemoRequestDto.UpdateDto request) {
        // JWT 인증 토큰을 발급하는 시스템이 아직 구현되어있지 않아서 구현한 임시코드입니다.
        User currentUser = userRepository.findById(1L)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_2622));

        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMO_8601));

        if (!memo.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new CustomException(ErrorCode.MEMO_8602);
        }

        /* 인증 시스템 구현 완료 되면 아래 코드로 수정 예정입니다.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = ((UserPrincipal) authentication.getPrincipal()).getId();
        if (!memo.getUser().getId().equals(currentUser.getId())) {
            throw new CustomException(ErrorCode.MEMO_8602);
        }
        */
        // 1. 메모에 연결된 기사 있는지 확인
        if (memo.getArticle() == null) {
            throw new CustomException(ErrorCode.MEMO_8603);
        }
        // 2. 경로로 받은 article_id와 메모의 article_id 가 일치하는지 확인
        //    (해당 코드 없으면 연결된 기사가 있기만 하면 메모 마구잡이로 수정됨)
        if(!memo.getArticle().getArticleId().equals(articleId)) {
            throw new CustomException(ErrorCode.MEMO_8606);
        }

        memo.updateContent(request.getContent());

        return MemoConverter.toUpdateResultDto(memo);
    }

    @Override
    public void deleteMemo(Long articleId, Long memoId) {
        // JWT 인증 토큰을 발급하는 시스템이 아직 구현되어있지 않아서 구현한 임시코드입니다.
        User currentUser = userRepository.findById(1L)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_2622));

        // 삭제할 메모 조회
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMO_8601));

        // 메모 소유권 확인
        if (!memo.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new CustomException(ErrorCode.MEMO_8602);
        }

        // 1. 메모에 연결된 기사 있는지 확인
        if (memo.getArticle() == null) {
            throw new CustomException(ErrorCode.MEMO_8603);
        }
        // 2. 경로로 받은 article_id와 메모의 article_id 가 일치하는지 확인
        //    (해당 코드 없으면 연결된 기사가 있기만 하면 메모 마구잡이로 수정됨)
        if(!memo.getArticle().getArticleId().equals(articleId)) {
            throw new CustomException(ErrorCode.MEMO_8606);
        }

        memoRepository.delete(memo);

        /* 인증 시스템 구현 완료 되면 아래 코드로 수정 예정입니다.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = ((UserPrincipal) authentication.getPrincipal()).getId();
        if (!memo.getUser().getId().equals(currentUser.getId())) {
            throw new CustomException(ErrorCode.MEMO_8602);
        }
        */
    }
}
