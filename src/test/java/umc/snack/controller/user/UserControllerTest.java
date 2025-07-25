package umc.snack.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import umc.snack.domain.user.dto.UserSignupRequestDto;
import umc.snack.domain.user.entity.User;
import umc.snack.repository.user.UserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
        entityManager.flush(); // 강제 flush!
    }

    @Test
    @DisplayName("회원가입 성공")
    void signup_success() throws Exception {
        UserSignupRequestDto request = new UserSignupRequestDto(
                "testuser@example.com",
                "test1234",     // 영문+숫자 8자 이상
                "테스터"
        );

        mockMvc.perform(post("/api/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("USER_2501"))
                .andExpect(jsonPath("$.result.email").value("testuser@example.com"));
    }

    @Test
    @DisplayName("이메일 중복 회원가입 실패")
    void signup_fail_duplicate_email() throws Exception {
        // 먼저 가입
        userRepository.save(
                User.builder()
                        .email("dup@example.com")
                        .password("encodedpassword")
                        .nickname("중복테스트")
                        .build()
        );

        UserSignupRequestDto request = new UserSignupRequestDto(
                "dup@example.com",
                "validpw123",
                "다른닉네임"
        );

        mockMvc.perform(post("/api/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("USER_2601"));
    }

    @Test
    @DisplayName("닉네임 중복 회원가입 실패")
    void signup_fail_duplicate_nickname() throws Exception {
        // 먼저 가입
        userRepository.save(
                User.builder()
                        .email("newmail@example.com")
                        .password("encodedpassword")
                        .nickname("닉중복")
                        .build()
        );

        UserSignupRequestDto request = new UserSignupRequestDto(
                "unique@example.com",
                "validpw123",
                "닉중복"
        );

        mockMvc.perform(post("/api/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("USER_2602"));
    }

    @Test
    @DisplayName("비밀번호 형식 오류 회원가입 실패")
    void signup_fail_password_pattern() throws Exception {
        UserSignupRequestDto request = new UserSignupRequestDto(
                "pwtest@example.com",
                "1234567", // 8자 미만 + 영문 없음
                "비번실패"
        );

        mockMvc.perform(post("/api/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("USER_2603"));
    }

    @Test
    @DisplayName("닉네임 길이 오류 회원가입 실패")
    void signup_fail_nickname_length() throws Exception {
        UserSignupRequestDto request = new UserSignupRequestDto(
                "nicklenfail@example.com",
                "validpw123",
                "ㄱ" // 2자 미만
        );

        mockMvc.perform(post("/api/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("USER_2607")); // 2607 닉네임 길이 오류라고 가정
    }
}
