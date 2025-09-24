package umc.snack.service.report;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.report.dto.TermReportRequestDto;
import umc.snack.domain.report.dto.TermReportResponseDto;
import umc.snack.domain.report.entity.TermReport;
import umc.snack.domain.user.entity.User;
import umc.snack.repository.article.ArticleRepository;
import umc.snack.repository.report.TermReportRepository;
import umc.snack.repository.user.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class TermReportServiceImpl implements TermReportService {

    private final TermReportRepository termReportRepository;
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;

    @Override
    public TermReportResponseDto createReport(Long articleId, TermReportRequestDto requestDto, Long userIdFromToken) {
        // 1) userId는 인증된 토큰에서만 사용 (보안 강화)
        Long userId = userIdFromToken;
        if (userId == null || articleId == null) {
            throw new CustomException(ErrorCode.REPORT_8803);
        }

        // 2) reason 디폴트 값 설정
        String reason = "용어 설명에 오류가 있어서 신고합니다.";

        // 3) User, Article 존재 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_2622));
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new CustomException(ErrorCode.ARTICLE_9104_GET));

        // 4) 중복 신고 체크
        boolean exists = termReportRepository.existsByUser_UserIdAndArticle_ArticleId(userId, article.getArticleId());
        if (exists) {
            throw new CustomException(ErrorCode.REPORT_8801);
        }

        // 5) 저장 (reported는 기본적으로 true)
        TermReport report = TermReport.builder()
                .user(user)
                .article(article)
                .reported(true)
                .reason(reason)
                .build();

        TermReport saved = termReportRepository.save(report);
        return TermReportResponseDto.builder()
                .reportId(saved.getReportId())
                .reported(saved.isReported())
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public boolean hasReported(Long articleId, Long userIdFromToken) {
        if (userIdFromToken == null || articleId == null) {
            throw new CustomException(ErrorCode.REPORT_8803);
        }
        return termReportRepository.existsByUser_UserIdAndArticle_ArticleIdAndReportedTrue(userIdFromToken, articleId);
    }
}
