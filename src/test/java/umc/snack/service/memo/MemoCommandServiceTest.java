package umc.snack.service.memo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.memo.dto.MemoRequestDto;
import umc.snack.domain.memo.dto.MemoResponseDto;
import umc.snack.domain.memo.entity.Memo;
import umc.snack.domain.user.entity.User;
import umc.snack.repository.article.ArticleRepository;
import umc.snack.repository.memo.MemoRepository;
import umc.snack.repository.user.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemoCommandServiceImpl UnitTest")
class MemoCommandServiceImplTest {

    @InjectMocks
    private MemoCommandServiceImpl memoCommandService;

    @Mock
    private MemoRepository memoRepository;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private UserRepository userRepository;

    private User testUser;
    private Article testArticle;
    private Memo testMemo;

    @BeforeEach
    void setUp() {
        // 테스트에 사용할 Mock 객체 설정
        testUser = User.builder().userId(1L).build();
        testArticle = Article.builder().articleId(10L).build(); // articleUrl 필드 제거
        testMemo = Memo.builder().memoId(20L).content("테스트 메모").user(testUser).article(testArticle).build();
    }

    @Test
    @DisplayName("성공: 새로운 메모 생성")
    void createMemo_Success() {
        // given
        // MemoRequestDto.CreateDto 객체를 빌더 패턴으로 생성하도록 수정
        MemoRequestDto.CreateDto requestDto = MemoRequestDto.CreateDto.builder()
                .content("새로운 메모 내용")
                .build();
        when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
        when(articleRepository.findById(any())).thenReturn(Optional.of(testArticle));
        when(memoRepository.save(any(Memo.class))).thenReturn(testMemo);

        // when
        MemoResponseDto.CreateResultDto result = memoCommandService.createMemo(testArticle.getArticleId(), requestDto, testUser.getUserId());

        // then
        assertNotNull(result);
        assertEquals(testMemo.getMemoId(), result.getMemoId());
        verify(memoRepository, times(1)).save(any(Memo.class));
    }

    @Test
    @DisplayName("실패: 메모 생성시 Article이 존재하지 않음")
    void createMemo_ArticleNotFound() {
        // given
        // MemoRequestDto.CreateDto 객체를 빌더 패턴으로 생성하도록 수정
        MemoRequestDto.CreateDto requestDto = MemoRequestDto.CreateDto.builder()
                .content("새로운 메모 내용")
                .build();
        when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
        when(articleRepository.findById(any())).thenReturn(Optional.empty());

        // when & then
        CustomException exception = assertThrows(CustomException.class,
                () -> memoCommandService.createMemo(testArticle.getArticleId(), requestDto, testUser.getUserId()));
        assertEquals(ErrorCode.MEMO_8605, exception.getErrorCode());
    }

    @Test
    @DisplayName("성공: 메모 수정")
    void updateMemo_Success() {
        // given
        // MemoRequestDto.UpdateDto 객체를 빌더 패턴으로 생성하도록 수정
        MemoRequestDto.UpdateDto requestDto = MemoRequestDto.UpdateDto.builder()
                .content("수정된 메모 내용")
                .build();
        when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
        when(memoRepository.findById(any())).thenReturn(Optional.of(testMemo));

        // when
        MemoResponseDto.UpdateResultDto result = memoCommandService.updateMemo(
                testArticle.getArticleId(), testMemo.getMemoId(), requestDto, testUser.getUserId());

        // then
        assertNotNull(result);
        assertEquals(requestDto.getContent(), result.getContent());
        assertEquals(testMemo.getMemoId(), result.getMemoId());
    }

    @Test
    @DisplayName("실패: 메모 수정시 메모 소유자가 아님")
    void updateMemo_UserNotOwner() {
        // given
        User otherUser = User.builder().userId(2L).build();
        // MemoRequestDto.UpdateDto 객체를 빌더 패턴으로 생성하도록 수정
        MemoRequestDto.UpdateDto requestDto = MemoRequestDto.UpdateDto.builder()
                .content("수정된 메모 내용")
                .build();
        when(userRepository.findById(any())).thenReturn(Optional.of(otherUser));
        when(memoRepository.findById(any())).thenReturn(Optional.of(testMemo));

        // when & then
        CustomException exception = assertThrows(CustomException.class,
                () -> memoCommandService.updateMemo(testArticle.getArticleId(), testMemo.getMemoId(), requestDto, otherUser.getUserId()));
        assertEquals(ErrorCode.MEMO_8602, exception.getErrorCode());
    }

    @Test
    @DisplayName("성공: 메모 삭제")
    void deleteMemo_Success() {
        // given
        when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
        when(memoRepository.findById(any())).thenReturn(Optional.of(testMemo));
        doNothing().when(memoRepository).delete(any(Memo.class));

        // when
        memoCommandService.deleteMemo(testArticle.getArticleId(), testMemo.getMemoId(), testUser.getUserId());

        // then
        verify(memoRepository, times(1)).delete(any(Memo.class));
    }
}
