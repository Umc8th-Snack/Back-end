package umc.snack.controller.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import umc.snack.common.config.security.jwt.JWTUtil;
import umc.snack.domain.auth.entity.RefreshToken;
import umc.snack.repository.auth.RefreshTokenRepository;

import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerReissueTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JWTUtil jwtUtil;
    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
    }

    @DisplayName("유효한 refreshToken 쿠키로 access/refresh 재발급 성공")
    @Test
    void reissue_success() throws Exception {
        // given
        Long userId = 123L;
        String role = "ROLE_USER";
        String validRefreshToken = jwtUtil.createJwt("refresh", userId, role, 86_400_000L);

        // refresh 토큰 DB 저장(화이트리스트)
        refreshTokenRepository.save(
                RefreshToken.builder()
                        .userId(userId)
                        .email("test@example.com")
                        .refreshToken(validRefreshToken)
                        .expiration(LocalDateTime.now().plusDays(1))
                        .build()
        );

        // when & then
        mockMvc.perform(post("/api/auth/reissue")
                        .cookie(new Cookie("refresh", validRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(header().exists("access"))
                .andExpect(cookie().exists("refresh"));
    }

    @DisplayName("refreshToken이 없는 경우 400 에러")
    @Test
    void reissue_noRefreshToken() throws Exception {
        mockMvc.perform(post("/api/auth/reissue"))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("만료된 refreshToken으로 재발급 시도시 400 에러")
    @Test
    void reissue_expiredToken() throws Exception {
        Long userId = 999L;
        String role = "ROLE_USER";
        // 이미 만료된 토큰
        String expiredToken = jwtUtil.createJwt("refresh", userId, role, -1L);

        refreshTokenRepository.save(
                RefreshToken.builder()
                        .userId(userId)
                        .email("test@example.com")
                        .refreshToken(expiredToken)
                        .expiration(LocalDateTime.now().minusDays(1))
                        .build()
        );

        mockMvc.perform(post("/api/auth/reissue")
                        .cookie(new Cookie("refresh", expiredToken)))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("DB에 없는 refreshToken으로 재발급 시도시 400 에러")
    @Test
    void reissue_tokenNotInDB() throws Exception {
        Long userId = 999L;
        String role = "ROLE_USER";
        String fakeToken = jwtUtil.createJwt("refresh", userId, role, 86_400_000L);

        // 일부러 DB에 저장하지 않음

        mockMvc.perform(post("/api/auth/reissue")
                        .cookie(new Cookie("refresh", fakeToken)))
                .andExpect(status().isBadRequest());
    }
}
