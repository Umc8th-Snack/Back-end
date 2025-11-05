package umc.snack.service.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import umc.snack.common.config.security.jwt.JWTUtil;
import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;
import umc.snack.domain.auth.dto.SocialLoginResponseDto;
import umc.snack.domain.auth.entity.RefreshToken;
import umc.snack.domain.user.entity.User;
import umc.snack.repository.auth.RefreshTokenRepository;
import umc.snack.repository.user.UserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoOAuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JWTUtil jwtUtil;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.jwt.token.expiration.access}")
    private Long accessExpiredMs;

    @Value("${spring.jwt.token.expiration.refresh}")
    private Long refreshExpiredMs;

    @Value("${kakao.oauth.client-id:}")
    private String kakaoClientId;

    @Value("${kakao.oauth.client-secret:}")
    private String kakaoClientSecret;

    @Value("${kakao.oauth.redirect-uri:}")
    private String kakaoRedirectUri;

    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String KAKAO_PLACEHOLDER_EMAIL_DOMAIN = "kakao-user.snacknews";

    @Transactional
    public SocialLoginResponseDto processKakaoCallback(String authorizationCode) {
        try {
            // 1. 카카오로부터 액세스 토큰 받기
            String kakaoAccessToken = getKakaoAccessToken(authorizationCode);
            
            // 2. 카카오 사용자 정보 조회
            KakaoUserInfo userInfo = getKakaoUserInfo(kakaoAccessToken);
            
            // 3. 사용자 로그인/회원가입 처리
            User user = findOrCreateUser(userInfo);

            // 4. JWT 토큰 생성
            String accessToken = jwtUtil.createJwt("access", user.getUserId(), user.getEmail(), user.getRole().name(), accessExpiredMs);
            String refreshToken = jwtUtil.createJwt("refresh", user.getUserId(), user.getEmail(), user.getRole().name(), refreshExpiredMs);

            // 5. Refresh Token 저장
            saveRefreshToken(user.getUserId(), user.getEmail(), refreshToken);
            
            // 6. 소셜 로그인 정보 저장/업데이트
            saveSocialLoginInfo(user.getUserId(), kakaoAccessToken, userInfo.getId());

            return SocialLoginResponseDto.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("카카오 OAuth 처리 중 예상치 못한 오류", e);
            throw new CustomException(ErrorCode.AUTH_2115);
        }
    }

    private String getKakaoAccessToken(String authorizationCode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoClientId);
        params.add("client_secret", kakaoClientSecret);
        params.add("code", authorizationCode);
        params.add("redirect_uri", kakaoRedirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    KAKAO_TOKEN_URL,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return jsonNode.get("access_token").asText();
            } else {
                log.error("카카오 액세스 토큰 획득 실패 - 응답 상태: {}", response.getStatusCode());
                throw new CustomException(ErrorCode.AUTH_2116);
            }
        } catch (HttpClientErrorException e) {
            log.error("카카오 액세스 토큰 획득 실패 - Client Error: {}", e.getMessage());
            throw new CustomException(ErrorCode.AUTH_2116);
        } catch (HttpServerErrorException e) {
            log.error("카카오 액세스 토큰 획득 실패 - Server Error: {}", e.getMessage());
            throw new CustomException(ErrorCode.AUTH_2115);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("카카오 액세스 토큰 획득 실패", e);
            throw new CustomException(ErrorCode.AUTH_2115);
        }
    }

    private KakaoUserInfo getKakaoUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    KAKAO_USER_INFO_URL,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            String kakaoId = jsonNode.path("id").asText(null);
            if (kakaoId == null) {
                log.warn("카카오 사용자 정보에 ID가 없습니다.");
                throw new CustomException(ErrorCode.AUTH_2118);
            }

            JsonNode kakaoAccount = jsonNode.path("kakao_account");
            if (kakaoAccount.isMissingNode()) {
                log.warn("카카오 사용자 정보에 kakao_account 노드가 없습니다.");
                throw new CustomException(ErrorCode.AUTH_2118);
            }

            String email = resolveEmail(kakaoAccount, kakaoId);

            JsonNode profileNode = kakaoAccount.path("profile");
            if (profileNode.isMissingNode() || profileNode.isNull()) {
                log.warn("카카오 사용자 정보에 profile 노드가 없습니다.");
                profileNode = objectMapper.createObjectNode();
            }
            String nickname = profileNode.path("nickname").asText(null);
            String profileImage = profileNode.path("profile_image_url").asText(null);
            if (profileImage == null || profileImage.isBlank()) {
                profileImage = profileNode.path("thumbnail_image_url").asText(null);
            }

            return KakaoUserInfo.builder()
                    .id(kakaoId)
                    .email(email)
                    .nickname(nickname)
                    .profileImage(profileImage)
                    .build();

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new CustomException(ErrorCode.AUTH_2116);
            }
            log.error("카카오 사용자 정보 조회 실패 - Client Error", e);
            throw new CustomException(ErrorCode.AUTH_2118);
        } catch (HttpServerErrorException e) {
            log.error("카카오 사용자 정보 조회 실패 - Server Error", e);
            throw new CustomException(ErrorCode.AUTH_2115);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("카카오 사용자 정보 파싱 실패", e);
            throw new CustomException(ErrorCode.AUTH_2118);
        }
    }

    private User findOrCreateUser(KakaoUserInfo userInfo) {
        Optional<User> existingUserOpt = userRepository.findByEmail(userInfo.getEmail());

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if (existingUser.getLoginType() == null) {
                existingUser.setLoginType(User.LoginType.KAKAO);
            }
            return existingUser;
        }

        String nickname = generateUniqueNickname(userInfo.getNickname(), userInfo.getEmail());

        User newUser = User.builder()
                .email(userInfo.getEmail())
                .password("SOCIAL_LOGIN_USER")
                .nickname(nickname)
                .profileImage(userInfo.getProfileImage())
                .status(User.Status.ACTIVE)
                .role(User.Role.ROLE_USER)
                .loginType(User.LoginType.KAKAO)
                .build();

        return userRepository.save(newUser);
    }

    private String resolveEmail(JsonNode kakaoAccount, String kakaoId) {
        String email = kakaoAccount.path("email").asText(null);
        if (email != null && !email.isBlank()) {
            return email;
        }

        boolean hasEmail = kakaoAccount.path("has_email").asBoolean(false);
        boolean needsAgreement = kakaoAccount.path("email_needs_agreement").asBoolean(false);

        if (hasEmail && needsAgreement) {
            log.warn("카카오 계정 이메일 동의가 필요하지만 제공되지 않았습니다. Kakao ID: {}", kakaoId);
        } else {
            log.warn("카카오 계정에 이메일 정보가 제공되지 않았습니다. Kakao ID: {}", kakaoId);
        }

        return buildPlaceholderEmail(kakaoId);
    }

    private String buildPlaceholderEmail(String kakaoId) {
        return String.format("kakao-%s@%s", kakaoId, KAKAO_PLACEHOLDER_EMAIL_DOMAIN);
    }

    private String generateUniqueNickname(String rawNickname, String email) {
        String base = sanitizeNickname(rawNickname);
        if (base.isBlank()) {
            base = sanitizeNickname(email != null ? email.split("@")[0] : "kakao");
        }
        if (base.length() < 2) {
            base = "kakao";
        }

        String candidate = truncateNickname(base);
        int suffix = 1;
        while (userRepository.existsByNickname(candidate)) {
            String suffixStr = String.valueOf(suffix++);
            String truncatedBase = truncateNickname(base, suffixStr.length());
            candidate = truncatedBase + suffixStr;
        }
        if (candidate.length() < 2) {
            candidate = candidate + "0";
        }
        return candidate;
    }

    private String sanitizeNickname(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^가-힣a-zA-Z0-9]", "");
    }

    private String truncateNickname(String base) {
        return truncateNickname(base, 0);
    }

    private String truncateNickname(String base, int reserveForSuffix) {
        int maxLength = Math.max(2, 6 - reserveForSuffix);
        if (base.length() > maxLength) {
            return base.substring(0, maxLength);
        }
        return base;
    }

    private void saveRefreshToken(Long userId, String email, String refreshToken) {
        refreshTokenRepository.deleteByUserId(userId);

        RefreshToken token = RefreshToken.builder()
                .userId(userId)
                .email(email)
                .refreshToken(refreshToken)
                .expiration(LocalDateTime.now().plusSeconds(refreshExpiredMs / 1000))
                .build();
        refreshTokenRepository.save(token);
    }

    private void saveSocialLoginInfo(Long userId, String kakaoAccessToken, String kakaoUserId) {
        log.info("카카오 소셜 로그인 - 사용자 ID: {}, Kakao ID: {}", userId, kakaoUserId);
    }

    @Builder
    @Getter
    private static class KakaoUserInfo {
        private final String id;
        private final String email;
        private final String nickname;
        private final String profileImage;
    }
}


