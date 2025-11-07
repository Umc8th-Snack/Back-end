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
import umc.snack.repository.auth.SocialLoginRepository;
import umc.snack.repository.user.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SocialLoginRepository socialLoginRepository;
    private final JWTUtil jwtUtil;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.jwt.token.expiration.access}")
    private Long accessExpiredMs;

    @Value("${spring.jwt.token.expiration.refresh}")
    private Long refreshExpiredMs;

    @Value("${google.oauth.client-id:}")
    private String googleClientId;

    @Value("${google.oauth.client-secret:}")
    private String googleClientSecret;

    @Value("${google.oauth.redirect-uri:}")
    private String googleRedirectUri;

    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
    private static final String GOOGLE_REVOKE_URL = "https://oauth2.googleapis.com/revoke";

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
            String accessToken = jwtUtil.createJwt("access", user.getUserId(), user.getEmail(), user.getRole().name(), accessExpiredMs); // 30분
            String refreshToken = jwtUtil.createJwt("refresh", user.getUserId(), user.getEmail(), user.getRole().name(), refreshExpiredMs); // 1일
            
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
        Optional<User> existingUserOpt = userRepository.findByEmail(userInfo.getEmail());

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            // loginType이 설정되어 있지 않은 경우에만 GOOGLE로 세팅
            if (existingUser.getLoginType() == null) {
                existingUser.setLoginType(User.LoginType.GOOGLE);
            }
            // 이미 LOCAL이면 변경하지 않고 반환
            return existingUser;
        } else {
            // 신규 소셜 로그인 회원
            User newUser = User.builder()
                    .email(userInfo.getEmail())
                    .password(null)
                    .nickname(userInfo.getName())
                    .role(User.Role.ROLE_USER)
                    .status(User.Status.ACTIVE)
                    .loginType(User.LoginType.GOOGLE)
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
                .expiration(LocalDateTime.now().plusSeconds(refreshExpiredMs / 1000)) // 1일
                .build();
        refreshTokenRepository.save(token);
    }

    private void saveSocialLoginInfo(Long userId, String googleAccessToken, String googleUserId) {
        // 기존 소셜 로그인 정보 조회
        Optional<SocialLogin> existingOpt = socialLoginRepository.findByUserIdAndProvider(userId, "GOOGLE");
        
        if (existingOpt.isPresent()) {
            // 기존 정보 업데이트 (액세스 토큰 갱신)
            SocialLogin existing = existingOpt.get();
            SocialLogin updated = SocialLogin.builder()
                    .socialLoginId(existing.getSocialLoginId())
                    .userId(userId)
                    .provider("GOOGLE")
                    .providerSocialId(googleUserId)
                    .accessToken(googleAccessToken)
                    .build();
            socialLoginRepository.save(updated);
        } else {
            // 신규 소셜 로그인 정보 저장
            SocialLogin socialLogin = SocialLogin.builder()
                    .userId(userId)
                    .provider("GOOGLE")
                    .providerSocialId(googleUserId)
                    .accessToken(googleAccessToken)
                    .build();
            socialLoginRepository.save(socialLogin);
        }
        
        log.info("구글 소셜 로그인 정보 저장 완료 - 사용자 ID: {}, Google ID: {}", userId, googleUserId);
    }
    
    /**
     * 구글 토큰 해제 (회원 탈퇴 시 호출)
     */
    @Transactional
    public void revokeToken(Long userId) {
        try {
            // 저장된 소셜 로그인 정보 조회
            Optional<SocialLogin> socialLoginOpt = socialLoginRepository.findByUserIdAndProvider(userId, "GOOGLE");
            
            if (socialLoginOpt.isEmpty()) {
                log.warn("구글 소셜 로그인 정보가 없습니다. 사용자 ID: {}", userId);
                // 정보가 없어도 탈퇴는 진행되어야 하므로 예외를 던지지 않음
                return;
            }
            
            SocialLogin socialLogin = socialLoginOpt.get();
            String accessToken = socialLogin.getAccessToken();
            
            if (accessToken == null || accessToken.isBlank()) {
                log.warn("구글 액세스 토큰이 없습니다. 사용자 ID: {}", userId);
                // 토큰이 없어도 DB 정보는 삭제
                socialLoginRepository.deleteByUserId(userId);
                return;
            }
            
            // 구글 토큰 해제 API 호출
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("token", accessToken);
            
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
            
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        GOOGLE_REVOKE_URL,
                        HttpMethod.POST,
                        entity,
                        String.class
                );
                
                if (response.getStatusCode() == HttpStatus.OK) {
                    log.info("구글 토큰 해제 성공 - 사용자 ID: {}", userId);
                } else {
                    log.warn("구글 토큰 해제 응답 이상 - 사용자 ID: {}, 상태: {}", userId, response.getStatusCode());
                }
            } catch (Exception e) {
                // 토큰이 이미 만료되었거나 유효하지 않은 경우
                log.warn("구글 토큰 해제 실패 - 사용자 ID: {}, 에러: {}", userId, e.getMessage());
            }
            
            // API 호출 성공 여부와 관계없이 DB에서 소셜 로그인 정보 삭제
            socialLoginRepository.deleteByUserId(userId);
            log.info("구글 소셜 로그인 정보 삭제 완료 - 사용자 ID: {}", userId);
            
        } catch (Exception e) {
            log.error("구글 토큰 해제 처리 중 오류 발생 - 사용자 ID: {}", userId, e);
            // 탈퇴는 계속 진행되어야 하므로 예외를 던지지 않음
        }
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