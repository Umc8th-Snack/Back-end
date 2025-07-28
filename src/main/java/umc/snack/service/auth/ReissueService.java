package umc.snack.service.auth;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import umc.snack.common.config.security.jwt.JWTUtil;
import umc.snack.common.exception.ErrorCode;
import umc.snack.common.response.ApiResponse;
import umc.snack.domain.auth.dto.TokenReissueResponseDto;
import umc.snack.domain.auth.entity.RefreshToken;
import umc.snack.domain.user.entity.User;
import umc.snack.repository.auth.RefreshTokenRepository;
import umc.snack.repository.user.UserRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReissueService {

    private final JWTUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public ResponseEntity<ApiResponse<TokenReissueResponseDto>> reissue(HttpServletRequest request, HttpServletResponse response) {
        // 1. 쿠키에서 refresh token 추출
        String refreshToken = extractRefreshTokenFromCookies(request);

        if (refreshToken == null) {
            // Refresh 토큰이 존재하지 않음
            return buildFail(ErrorCode.AUTH_2163, "AUTH_2163");
        }

        // 2. DB에 refresh token 존재 확인 (화이트리스트 방식)
        var foundOpt = refreshTokenRepository.findByRefreshToken(refreshToken);
        if (foundOpt.isEmpty()) {
            // 서버에 해당 Refresh 토큰이 없음
            return buildFail(ErrorCode.AUTH_2165, "AUTH_2165");
        }
        RefreshToken found = foundOpt.get();

        // 3. 만료 여부 체크
        try {
            jwtUtil.isExpired(refreshToken);
        } catch (ExpiredJwtException e) {
            // Refresh 토큰 만료
            return buildFail(ErrorCode.AUTH_2164, "AUTH_2164");
        }

        // 4. refresh 토큰 유효성 및 category 체크
        String category = jwtUtil.getCategory(refreshToken);
        if (!"refresh".equals(category)) {
            // 유효하지 않은 Refresh 토큰 (payload category 틀림)
            return buildFail(ErrorCode.AUTH_2162, "AUTH_2162");
        }

        // 5. user 정보 파싱 및 조회
        Long userId = jwtUtil.getUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElse(null);
        if (user == null) {
            // 등록되지 않은 이메일(계정)
            return buildFail(ErrorCode.AUTH_2141, "AUTH_2141");
        }

        // 6. 토큰 재발급 (access + refresh)
        String role = jwtUtil.getRole(refreshToken);
        String newAccess = jwtUtil.createJwt("access", userId, role, 1_800_000L);         // 30분
        String newRefresh = jwtUtil.createJwt("refresh", userId, role, 86_400_000L);    // 1일

        // 기존 refreshToken 폐기, 새로운 refreshToken 저장 (화이트리스트 정책)
        refreshTokenRepository.delete(found);
        refreshTokenRepository.save(
                RefreshToken.builder()
                        .userId(userId)
                        .email(user.getEmail())
                        .refreshToken(newRefresh)
                        .expiration(found.getExpiration().plusDays(1))
                        .build()
        );

        // access 토큰은 header, refresh 토큰은 쿠키
        response.setHeader("access", newAccess);
        response.addCookie(createCookie("refresh", newRefresh));

        // 응답 Dto 만들기
        TokenReissueResponseDto dto = TokenReissueResponseDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .accessToken(newAccess)
                .refreshToken(newRefresh)
                .build();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "AUTH_2060",
                        "Access 토큰이 재발급되었습니다.",
                        dto
                )
        );
    }

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

    private Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24 * 60 * 60);
        cookie.setHttpOnly(true);
        return cookie;
    }

    private ResponseEntity<ApiResponse<TokenReissueResponseDto>> buildFail(ErrorCode errorCode, String code) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(
                        code,
                        errorCode.getMessage(),
                        null
                ));
    }


    @Transactional
    public void replaceRefreshToken(Long userId, String email, String refreshToken, LocalDateTime expirationDate) {
        refreshTokenRepository.deleteByUserId(userId);

        RefreshToken entity = new RefreshToken();
        entity.setUserId(userId);
        entity.setEmail(email);
        entity.setRefreshToken(refreshToken);
        entity.setExpiration(expirationDate);

        refreshTokenRepository.save(entity);
    }
}