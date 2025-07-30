package umc.snack.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import umc.snack.domain.auth.dto.LoginRequestDto;
import umc.snack.domain.user.entity.User;
import umc.snack.repository.user.UserRepository;
import umc.snack.repository.scrap.UserScrapRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserScrapRepository userScrapRepository;

    @BeforeEach
    void setUp() {
        userScrapRepository.deleteAll(); // 연관된 스크랩 먼저 삭제
        userRepository.deleteAll(); // 이후 사용자 삭제

        String rawPassword = "password123!";
        String encoded = passwordEncoder.encode(rawPassword);
        System.out.println("평문: " + rawPassword);
        System.out.println("암호화: " + encoded);
        User user = User.builder()
                .email("user1@example.com")
                .password(encoded)
                .nickname("테스트닉네임")
                .status(User.Status.ACTIVE)
                .role(User.Role.ROLE_USER)
                .build();
        userRepository.save(user);

    }
    @Test
    void 로그인_성공_테스트() throws Exception {
        // given
        LoginRequestDto requestDto = new LoginRequestDto("user1@example.com", "password123!");

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    void 로그인_실패_테스트() throws Exception {
        // 비밀번호 틀림
        LoginRequestDto requestDto = new LoginRequestDto("user1@example.com", "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }


}