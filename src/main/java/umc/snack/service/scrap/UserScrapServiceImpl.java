package umc.snack.service.scrap;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.user.entity.User;
import umc.snack.domain.user.entity.UserScrap;
import umc.snack.repository.article.ArticleRepository;
import umc.snack.repository.scrap.UserScrapRepository;
import umc.snack.repository.user.UserRepository;

import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;

@Service
@RequiredArgsConstructor
public class UserScrapServiceImpl implements UserScrapService {

    private final UserScrapRepository userScrapRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void addScrap(Long userId, Long articleId) {
        try {
            // 이미 해당 사용자가 해당 기사를 스크랩했는지 확인
            if (userScrapRepository.existsByUserIdAndArticleId(userId, articleId)) {
                throw new CustomException(ErrorCode.SCRAP_6101);
            }

            // 기사 ID로 기사 조회 (없으면 예외 발생)
            Article article = articleRepository.findById(articleId)
                    .orElseThrow(() -> new CustomException(ErrorCode.SCRAP_6103));

            // 사용자 ID로 사용자 조회 (없으면 예외 발생)
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_2622));

            // UserScrap 객체 생성 및 저장
            UserScrap scrap = UserScrap.builder()
                    .user(user)
                    .article(article)
                    .build();

            userScrapRepository.save(scrap);
        } catch (CustomException e) {
            throw e; // 비즈니스 로직 예외는 그대로 다시 던짐
        } catch (Exception e) {
            throw new CustomException(ErrorCode.SERVER_5101); // 일반적인 서버 오류 처리
        }
    }

    @Override
    @Transactional
    public void cancelScrap(Long userId, Long articleId) {
        try {
            // 해당 사용자의 스크랩 정보를 조회 (없으면 예외 발생)
            UserScrap scrap = userScrapRepository.findByUserIdAndArticleId(userId, articleId)
                    .orElseThrow(() -> new CustomException(ErrorCode.SCRAP_6105));

            // 스크랩 삭제
            userScrapRepository.delete(scrap);
        } catch (CustomException e) {
            throw e; // 비즈니스 예외는 그대로 던짐
        } catch (Exception e) {
            throw new CustomException(ErrorCode.SERVER_5101); // 서버 내부 오류로 감싸서 던짐
        }
    }

    @Override
    public Page<UserScrap> getScrapList(Long userId, Pageable pageable) {
        try {
            // createdAt 기준 최신순 정렬을 적용한 Pageable 객체 생성
            Pageable sortedPageable = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    org.springframework.data.domain.Sort.by("createdAt").descending()
            );

            // 사용자 ID 기준으로 스크랩 목록 조회
            return userScrapRepository.findAllByUserId(userId, sortedPageable);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.SERVER_5101); // 서버 내부 오류 처리
        }
    }

    @Override
    public boolean isScrapped(Long userId, Long articleId) {
        try {
            return userScrapRepository.existsByUserIdAndArticleId(userId, articleId);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.SERVER_5101); // 서버 내부 오류 처리
        }
    }

    @Override
    public String getArticleUrlByScrapIdAndUserId(Long scrapId, Long userId) {
        try {
            UserScrap userScrap = userScrapRepository.findById(scrapId)
                    .orElseThrow(() -> new CustomException(ErrorCode.SCRAP_6105)); // 스크랩 없음

            if (userScrap.getUser() == null || !userScrap.getUser().getUserId().equals(userId)) {
                throw new CustomException(ErrorCode.SCRAP_6106);
            }

            return "/api/articles/" + userScrap.getArticle().getArticleId(); // 내부 URI
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.SERVER_5101);
        }
    }
}
