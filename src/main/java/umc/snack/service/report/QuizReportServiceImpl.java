package umc.snack.service.report;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.report.dto.QuizReportRequestDto;
import umc.snack.domain.report.dto.QuizReportResponseDto;
import umc.snack.domain.report.entity.QuizReport;
import umc.snack.domain.user.entity.User;
import umc.snack.repository.article.ArticleRepository;
import umc.snack.repository.report.QuizReportRepository;
import umc.snack.repository.user.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizReportServiceImpl implements QuizReportService {

    private final QuizReportRepository quizReportRepository;
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;

    @Override
    public QuizReportResponseDto createReport(QuizReportRequestDto requestDto, Long userIdFromToken) {
        // 1) userId는 인증된 토큰에서만 사용 (보안 강화)
        Long userId = userIdFromToken;
        if (userId == null || requestDto.getArticleId() == null) {
            throw new CustomException(ErrorCode.REPORT_8803);
        }

        // 2) Optional reported default true
//        Boolean reported = requestDto.getReported();
//        if (reported == null) {
//            reported = true;
//        }

        // 3) reason length 검증은 DTO @Size로 1차, 여기서는 방어적 체크
        String reason = requestDto.getReason();
        if (reason != null && reason.length() > 1000) {
            throw new CustomException(ErrorCode.REPORT_8802);
        }

        // 4) User, Article 존재 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_2622));
        Article article = articleRepository.findById(requestDto.getArticleId())
                .orElseThrow(() -> new CustomException(ErrorCode.ARTICLE_9104_GET));

        // 5) 중복 신고 체크
        boolean exists = quizReportRepository.existsByUserIdAndArticleId(userId, article.getArticleId());
        if (exists) {
            throw new CustomException(ErrorCode.REPORT_8801);
        }

        // 6) 저장
        QuizReport report = QuizReport.builder()
                .userId(userId)
                .articleId(article.getArticleId())
//                .reported(reported)
                .reason(reason)
                .build();

        QuizReport saved = quizReportRepository.save(report);
        return QuizReportResponseDto.builder()
                .reportId(saved.getReportId())
                .build();
    }
}


