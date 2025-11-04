package umc.snack.common.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import umc.snack.common.config.security.CustomAuthenticationEntryPoint;
import umc.snack.common.config.security.jwt.JWTFilter;
import umc.snack.common.config.security.jwt.JWTUtil;
import umc.snack.common.config.security.jwt.LoginFilter;
import umc.snack.repository.auth.RefreshTokenRepository;
import umc.snack.repository.user.UserRepository;
import umc.snack.service.auth.ReissueService;

import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final JWTUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ReissueService reissueService;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Value("${spring.jwt.token.expiration.access}")
    private Long accessExpiredMs;

    @Value("${spring.jwt.token.expiration.refresh}")
    private Long refreshExpiredMs;

    public SecurityConfig(AuthenticationConfiguration authenticationConfiguration, JWTUtil jwtUtil, ReissueService reissueService ,RefreshTokenRepository refreshTokenRepository, CustomAuthenticationEntryPoint customAuthenticationEntryPoint) {

        this.authenticationConfiguration = authenticationConfiguration;
        this.jwtUtil = jwtUtil;
        this.refreshTokenRepository = refreshTokenRepository;
        this.reissueService = reissueService;
        this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, UserRepository userRepository) throws Exception {
        //csrf disable
        http
                .csrf((auth) -> auth.disable());

        //Form 로그인 방식 disable
        http
                .formLogin((auth) -> auth.disable());

        //http basic 인증 방식 disable
        http
                .httpBasic((auth) -> auth.disable());

        http
                .authorizeHttpRequests((auth) -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/swagger-ui.html",
                                "/webjars/**",
                                // 전체 공개 api
                                "/api/users/signup",
                                "/api/auth/login",
                                "/api/auth/google/callback",
                                "/api/auth/kakao/authorize",
                                "/api/auth/kakao/callback",
                                "/api/auth/reissue",
                                "/api/articles/*/related-articles",
                                "/api/articles/search",
                                "/api/articles/main",
                                "/api/feeds/**",
                                "/api/nlp/search/**",
                                "/api/users/password-reset/**",
                                // 관리자 공개 api -> 개발 단계에서는 전체 공개
                                "/api/share/**",
                                "/api/articles/crawl/status",
                                "/api/articles/*/summarize",
                                "/api/terms",
                                //캐시 액츄에이터
                                "/actuator/**"
                        ).permitAll()
                        // 나머지는 모두 JWT 토큰 인증 필요
                        .anyRequest().authenticated()
                );

        http
                .addFilterBefore(new JWTFilter(jwtUtil, userRepository, refreshTokenRepository), LoginFilter.class);
        http
                .addFilterAt(new LoginFilter(
                                authenticationManager(authenticationConfiguration),
                                jwtUtil,
                                reissueService,
                                refreshTokenRepository,
                                accessExpiredMs,
                                refreshExpiredMs),
                        UsernamePasswordAuthenticationFilter.class)
                .cors(cors -> cors
                        .configurationSource(request -> {
                            CorsConfiguration config = new CorsConfiguration();
                            config.setAllowCredentials(true);
                            // 수정된 부분: "https://snacknews.site" 대신 "*"를 추가하거나, 모든 패턴을 허용
                            config.setAllowedOriginPatterns(Collections.singletonList("*"));
                            config.setAllowedHeaders(List.of(
                                    "Authorization",
                                    "Content-Type",
                                    "Accept",
                                    "Origin",
                                    "X-Requested-With"
                            ));
                            config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                            config.setExposedHeaders(List.of("Authorization"));
                            return config;
                        })
                );
        http
                .exceptionHandling((exceptions) -> exceptions
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                );
        //세션 설정
        http
                .sessionManagement((session) -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));


        return http.build();
    }
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }
}

