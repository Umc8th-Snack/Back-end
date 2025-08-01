package umc.snack.service.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import umc.snack.common.config.security.jwt.JWTUtil;
import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;
import umc.snack.domain.auth.dto.SocialLoginResponseDto;
import umc.snack.domain.auth.entity.RefreshToken;
import umc.snack.domain.auth.entity.SocialLogin;
import umc.snack.domain.user.entity.User;
import umc.snack.repository.auth.RefreshTokenRepository;
import umc.snack.repository.user.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JWTUtil jwtUtil;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${google.oauth.client-id:}")
    private String googleClientId;

    @Value("${google.oauth.client-secret:}")
    private String googleClientSecret;

    @Value("${google.oauth.redirect-uri:}")
    private String googleRedirectUri;

    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    @Transactional
    public SocialLoginResponseDto processGoogleCallback(String authorizationCode) {
        try {
            // 1. 구글로부터 액세스 토큰 받기
            String googleAccessToken = getGoogleAccessToken(authorizationCode);
            
            // 2. 구글 사용자 정보 조회
            GoogleUserInfo userInfo = getGoogleUserInfo(googleAccessToken);
            
            // 3. 사용자 로그인/회원가입 처리
            User user = findOrCreateUser(userInfo);
            
            // 4. JWT 토큰 생성
            String accessToken = jwtUtil.createJwt("access", user.getUserId(), user.getEmail(), user.getRole().name(), 1_800_000L); // 30분
            String refreshToken = jwtUtil.createJwt("refresh", user.getUserId(), user.getEmail(), user.getRole().name(), 86_400_000L); // 1일
            
            // 5. Refresh Token 저장
            saveRefreshToken(user.getUserId(), user.getEmail(), refreshToken);
            
            // 6. 소셜 로그인 정보 저장/업데이트
            saveSocialLoginInfo(user.getUserId(), googleAccessToken, userInfo.getId());
            
            return SocialLoginResponseDto.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
                    
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("구글 OAuth 처리 중 오류 발생", e);
            throw new CustomException(ErrorCode.AUTH_2113); // 구글 인증 서버와의 통신에 실패하였습니다.
        }
    }

    private String getGoogleAccessToken(String authorizationCode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", googleClientId);
        params.add("client_secret", googleClientSecret);
        params.add("code", authorizationCode);
        params.add("grant_type", "authorization_code");
        params.add("redirect_uri", googleRedirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    GOOGLE_TOKEN_URL,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return jsonNode.get("access_token").asText();
            } else {
                throw new CustomException(ErrorCode.AUTH_2112); // 유효하지 않은 인가 코드입니다.
            }
        } catch (Exception e) {
            log.error("구글 액세스 토큰 획득 실패", e);
            throw new CustomException(ErrorCode.AUTH_2113); // 구글 인증 서버와의 통신에 실패하였습니다.
        }
    }

    private GoogleUserInfo getGoogleUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    GOOGLE_USER_INFO_URL,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return GoogleUserInfo.builder()
                        .id(jsonNode.get("id").asText())
                        .email(jsonNode.get("email").asText())
                        .name(jsonNode.get("name").asText())
                        .build();
            } else {
                throw new CustomException(ErrorCode.AUTH_2114); // 구글 사용자 정보를 조회하지 못했습니다.
            }
        } catch (Exception e) {
            log.error("구글 사용자 정보 조회 실패", e);
            throw new CustomException(ErrorCode.AUTH_2114); // 구글 사용자 정보를 조회하지 못했습니다.
        }
    }

    private User findOrCreateUser(GoogleUserInfo userInfo) {
        Optional<User> existingUser = userRepository.findByEmail(userInfo.getEmail());
        
        if (existingUser.isPresent()) {
            return existingUser.get();
        } else {
            // 새 사용자 생성 (소셜 로그인 사용자는 비밀번호가 없으므로 임시 값 설정)
            User newUser = User.builder()
                    .email(userInfo.getEmail())
                    .password("SOCIAL_LOGIN_USER") // 소셜 로그인 사용자 임시 패스워드
                    .nickname(userInfo.getName())
                    .role(User.Role.ROLE_USER)
                    .status(User.Status.ACTIVE)
                    .build();
            return userRepository.save(newUser);
        }
    }

    private void saveRefreshToken(Long userId, String email, String refreshToken) {
        // 기존 리프레시 토큰 삭제
        refreshTokenRepository.deleteByUserId(userId);
        
        // 새 리프레시 토큰 저장
        RefreshToken token = RefreshToken.builder()
                .userId(userId)
                .email(email)
                .refreshToken(refreshToken)
                .expiration(LocalDateTime.now().plusSeconds(86_400_000L / 1000)) // 1일
                .build();
        refreshTokenRepository.save(token);
    }

    private void saveSocialLoginInfo(Long userId, String googleAccessToken, String googleUserId) {
        // 소셜 로그인 정보 저장은 나중에 필요시 구현
        // 현재는 JWT 토큰 생성만으로 충분
        log.info("소셜 로그인 정보 - 사용자 ID: {}, 제공자: google, 제공자 사용자 ID: {}", userId, googleUserId);
    }

    // 구글 사용자 정보를 담는 내부 클래스
    @lombok.Builder
    @lombok.Getter
    public static class GoogleUserInfo {
        private String id;
        private String email;
        private String name;
    }
}