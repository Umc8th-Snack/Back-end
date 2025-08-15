package umc.snack.service.auth;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import umc.snack.common.config.security.jwt.JWTUtil;
import umc.snack.common.exception.ErrorCode;
import umc.snack.common.dto.ApiResponse;
import umc.snack.repository.auth.RefreshTokenRepository;

@Service
@RequiredArgsConstructor
public class LogoutService {


    private final RefreshTokenRepository refreshTokenRepository;
    private final JWTUtil jwtUtil;

    @Transactional
    public ResponseEntity<ApiResponse<Object>> logout(HttpServletRequest request, HttpServletResponse response) {
        // 1. 쿠키에서 refresh token 추출
        String refreshToken = extractRefreshTokenFromCookies(request);

        if (refreshToken == null) {
            // [AUTH_2163] 쿠키에 refresh token 없음
            expireRefreshCookie(response);
            return buildFail(ErrorCode.AUTH_2163);
        }

        // 2. DB에 해당 refresh token이 존재하는지 확인
        var foundOpt = refreshTokenRepository.findByRefreshToken(refreshToken);
        if (foundOpt.isEmpty()) {
            // [AUTH_2132] 이미 로그아웃/없는 토큰
            expireRefreshCookie(response);
            return buildFail(ErrorCode.AUTH_2132);
        }

        // 3. refresh token 만료 확인
        try {
            jwtUtil.isExpired(refreshToken);
        } catch (ExpiredJwtException e) {
            // [AUTH_2164] 만료된 토큰
            refreshTokenRepository.deleteByRefreshToken(refreshToken);
            expireRefreshCookie(response);
            return buildFail(ErrorCode.AUTH_2164);
        }

        // 4. 정상 로그아웃 (refresh token 삭제 + 쿠키 만료)
        refreshTokenRepository.deleteByRefreshToken(refreshToken);
        expireRefreshCookie(response);

        // 5. 성공 응답
        return ResponseEntity.ok(
                ApiResponse.onSuccess(
                        "USER_2031",
                        "성공적으로 로그아웃 되었습니다.",
                        null
                )
        );
    }

    // 쿠키에서 refresh token 추출
    private String extractRefreshTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refresh".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    // refresh 쿠키 만료 처리
    private void expireRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refresh", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
//        cookie.setSecure(true);
//        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    // 실패 응답 빌더
    private ResponseEntity<ApiResponse<Object>> buildFail(ErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.onFailure(
                        errorCode.name(),
                        errorCode.getMessage(),
                        null
                ));
    }
}
