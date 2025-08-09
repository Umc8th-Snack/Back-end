package umc.snack.service.user;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import umc.snack.common.config.security.CustomUserDetails;
import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;
import umc.snack.domain.user.dto.UserInfoResponseDto;
import umc.snack.domain.user.dto.UserSignupRequestDto;
import umc.snack.domain.user.dto.UserSignupResponseDto;
import umc.snack.domain.user.dto.UserUpdateRequestDto;
import umc.snack.domain.user.dto.UserUpdateResponseDto;
import umc.snack.domain.user.entity.User;
import umc.snack.repository.auth.RefreshTokenRepository;
import umc.snack.repository.memo.MemoRepository;
import umc.snack.repository.scrap.UserScrapRepository;
import umc.snack.repository.user.UserRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemoRepository memoRepository;
    private final UserScrapRepository userScrapRepository;

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

    // 로그인 사용자 정보 조회
    public UserInfoResponseDto getCurrentUserInfo() {
        // SecurityContext에서 현재 로그인한 사용자 정보 가져오기
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = userDetails.getUser();

        return UserInfoResponseDto.fromEntity(currentUser);
    }

    @Transactional
    public void withdraw(User user, String password) {
        if (user == null) {
            throw new CustomException(ErrorCode.USER_2622); // 존재하지 않는 회원
        }

        User managedUser = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_2622)); // 존재하지 않는 회원

        // 비밀번호 검증 로직
        if (!passwordEncoder.matches(password, managedUser.getPassword())) {
            throw new CustomException(ErrorCode.USER_2623);
        }

        if (managedUser .getStatus() == User.Status.DELETED) { // 이미 탈퇴 처리된 회원
            throw new CustomException(ErrorCode.USER_2621);
        }

        managedUser.withdraw();
        // refresh 토큰, 탈퇴한 user관련 메모, 스크랩 DB에서 삭제
        refreshTokenRepository.deleteByUserId(managedUser.getUserId());
        userScrapRepository.deleteByUserId(managedUser.getUserId());
        memoRepository.deleteAllByUser_UserId(managedUser.getUserId());

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

    @Transactional
    public UserUpdateResponseDto updateMyInfo(UserUpdateRequestDto request) {
        // SecurityContext에서 현재 로그인한 사용자 정보 가져오기
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = userDetails.getUser();

        // 관리되는 엔티티 조회
        User managedUser = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_2622)); // 존재하지 않는 회원

        // 닉네임 변경 시 유효성 검사
        if (request.getNickname() != null) {
            // 닉네임 형식 검사
            if (!isValidNickname(request.getNickname())) {
                throw new CustomException(ErrorCode.USER_2607);
            }
            // 닉네임 중복 검사 (자신의 현재 닉네임과 다른 경우에만)
            if (!request.getNickname().equals(managedUser.getNickname()) && 
                userRepository.existsByNickname(request.getNickname())) {
                throw new CustomException(ErrorCode.USER_2641);
            }
        }

        // 사용자 정보 업데이트
        managedUser.updateUserInfo(request.getNickname(), request.getProfileImage(), request.getIntroduction());

        return UserUpdateResponseDto.fromEntity(managedUser);
    }
}

