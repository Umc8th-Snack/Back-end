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
import umc.snack.repository.user.UserRepository;
import umc.snack.domain.user.entity.User;

import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerReissueTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JWTUtil jwtUtil;
    @Autowired
    RefreshTokenRepository refreshTokenRepository;
    @Autowired
    UserRepository userRepository;

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @DisplayName("유효한 refreshToken 쿠키로 access/refresh 재발급 성공")
    @Test
    void reissue_success() throws Exception {
        // given
        User user = userRepository.save(
                User.builder()
                        .email("test@example.com")
                        .password("testpass")
                        .nickname("홍길동")
                        .role(User.Role.ROLE_USER)
                        .status(User.Status.ACTIVE)
                        .build()
        );
        Long userId = user.getUserId();
        String role = "ROLE_USER";
        String validRefreshToken = jwtUtil.createJwt("refresh", userId, role, 86_400_000L);

        // refresh 토큰 DB 저장(화이트리스트)
        refreshTokenRepository.save(
                RefreshToken.builder()
                        .userId(userId)
                        .email(user.getEmail())
                        .refreshToken(validRefreshToken)
                        .expiration(LocalDateTime.now().plusDays(1))
                        .build()
        );

        // when & then
        mockMvc.perform(post("/api/auth/reissue")
                        .cookie(new Cookie("refresh", validRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(header().exists("access"))
                .andExpect(cookie().exists("refresh"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("AUTH_2060"))
                .andExpect(jsonPath("$.message").value("Access 토큰이 재발급되었습니다."))
                .andExpect(jsonPath("$.result.userId").value(userId))
                .andExpect(jsonPath("$.result.email").value(user.getEmail()))
                .andExpect(jsonPath("$.result.nickname").value(user.getNickname()))
                .andExpect(jsonPath("$.result.accessToken").exists())
                .andExpect(jsonPath("$.result.refreshToken").exists());
    }

    @DisplayName("refreshToken이 없는 경우 401 에러")
    @Test
    void reissue_noRefreshToken() throws Exception {
        mockMvc.perform(post("/api/auth/reissue"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_2163"));
    }

    @DisplayName("만료된 refreshToken으로 재발급 시도시 401 에러")
    @Test
    void reissue_expiredToken() throws Exception {
        // given
        User user = userRepository.save(
                User.builder()
                        .email("test2@example.com")
                        .password("testpass")
                        .nickname("홍길동2")
                        .role(User.Role.ROLE_USER)
                        .status(User.Status.ACTIVE)
                        .build()
        );
        Long userId = user.getUserId();
        String role = "ROLE_USER";
        String expiredToken = jwtUtil.createJwt("refresh", userId, role, -1L);

        refreshTokenRepository.save(
                RefreshToken.builder()
                        .userId(userId)
                        .email(user.getEmail())
                        .refreshToken(expiredToken)
                        .expiration(LocalDateTime.now().minusDays(1))
                        .build()
        );

        mockMvc.perform(post("/api/auth/reissue")
                        .cookie(new Cookie("refresh", expiredToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_2164"));
    }

    @DisplayName("DB에 없는 refreshToken으로 재발급 시도시 401 에러")
    @Test
    void reissue_tokenNotInDB() throws Exception {
        User user = userRepository.save(
                User.builder()
                        .email("test3@example.com")
                        .password("testpass")
                        .nickname("홍길동3")
                        .role(User.Role.ROLE_USER)
                        .status(User.Status.ACTIVE)
                        .build()
        );
        Long userId = user.getUserId();
        String role = "ROLE_USER";
        String fakeToken = jwtUtil.createJwt("refresh", userId, role, 86_400_000L);

        // 일부러 DB에 저장하지 않음

        mockMvc.perform(post("/api/auth/reissue")
                        .cookie(new Cookie("refresh", fakeToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_2165"));
    }
}
