package umc.snack.service.user;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;
import umc.snack.domain.user.dto.UserSignupRequestDto;
import umc.snack.domain.user.dto.UserSignupResponseDto;
import umc.snack.domain.user.entity.User;
import umc.snack.repository.user.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserSignupResponseDto signup(UserSignupRequestDto request) {

        // 닉네임 형식 오류
        if (!isValidNickname(request.getNickname())) {
            throw new CustomException(ErrorCode.USER_2607);
        }
        // 비밀번호 형식 오류
        if (!isValidPassword(request.getPassword())) {
            throw new CustomException(ErrorCode.USER_2603);
        }
        // 이메일 형식 오류
        if (!isValidEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.USER_2604);
        }
        // 이메일 중복
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.USER_2601);
        }
        // 닉네임 중복
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new CustomException(ErrorCode.USER_2602);
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .nickname(request.getNickname())
                .status(User.Status.ACTIVE)
                .role(User.Role.ROLE_USER)
                .build();

        userRepository.save(user);

        return UserSignupResponseDto.fromEntity(user);
    }

    // 비밀번호 정규식 체크 메소드
    private boolean isValidPassword(String password) {
        // 최소 8자, 영문/숫자 각각 1개 이상 포함
        String regex = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$";
        return password != null && password.matches(regex);
    }
    // 닉네임 정규식 체크 메소드
    private boolean isValidNickname(String nickname) {
        // 한글, 영문, 숫자로만 이루어진 2~12자의 문자열
        String regex = "^[가-힣a-zA-Z0-9]{2,12}$";
        return nickname != null && nickname.matches(regex);
    }
    // 이메일 정규식 체크 메소드
    private boolean isValidEmail(String email) {
        String regex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email != null && email.matches(regex);
    }
}

