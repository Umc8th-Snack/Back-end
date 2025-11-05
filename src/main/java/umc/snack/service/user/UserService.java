package umc.snack.service.user;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import umc.snack.common.config.security.CustomUserDetails;
import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;
import umc.snack.domain.user.dto.*;
import umc.snack.domain.user.entity.User;
import umc.snack.domain.user.entity.VerificationCode;
import umc.snack.repository.auth.RefreshTokenRepository;
import umc.snack.repository.memo.MemoRepository;
import umc.snack.repository.scrap.UserScrapRepository;
import umc.snack.repository.user.UserRepository;
import umc.snack.repository.user.VerificationCodeRepository;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemoRepository memoRepository;
    private final UserScrapRepository userScrapRepository;
    private final JavaMailSender mailSender;
    private final VerificationCodeService verificationCodeService;
    private final VerificationCodeRepository verificationCodeRepository;

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
                .loginType(User.LoginType.LOCAL)
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

    // 회원탈퇴
    @Transactional
    public void withdraw(User user, String password) {
        if (user == null) {
            throw new CustomException(ErrorCode.USER_2622); // 존재하지 않는 회원
        }

        User managedUser = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_2622)); // 존재하지 않는 회원

        // 소셜 로그인 유저와 일반 로그인 유저 분기 처리
        if (managedUser.getLoginType() == User.LoginType.LOCAL) {
            // 일반 로그인: 비밀번호 검증 필요
            if (managedUser.getPassword() == null || managedUser.getPassword().isBlank()) {
                throw new CustomException(ErrorCode.USER_2612); // 비밀번호 없음
            }
            if (password == null || password.isBlank()) {
                throw new CustomException(ErrorCode.USER_2611); // 비밀번호 불일치
            }
            if (!passwordEncoder.matches(password, managedUser.getPassword())) {
                throw new CustomException(ErrorCode.USER_2611); // 비밀번호 불일치
            }
        }
        // 소셜 로그인(GOOGLE, KAKAO): 비밀번호 검증 없이 탈퇴 가능 (JWT 인증으로 본인 확인됨)

        managedUser.withdraw();
        // refresh 토큰, 탈퇴한 user관련 메모, 스크랩 DB에서 삭제
        refreshTokenRepository.deleteByUserId(managedUser.getUserId());
        userScrapRepository.deleteByUserId(managedUser.getUserId());
        memoRepository.deleteAllByUser_UserId(managedUser.getUserId());

    }

    // 이메일 변경
    @Transactional
    public EmailChangeResponseDto changeEmail(Long userId, EmailChangeRequestDto req) {
        // 입력 정규화
        String newEmail = req.getNewEmail() == null ? null : req.getNewEmail().trim().toLowerCase();
        String currentPw = req.getCurrentPassword() == null ? null : req.getCurrentPassword().trim();

        // 필수값 / 형식 검증
        if (newEmail == null || newEmail.isEmpty()) {
            throw new CustomException(ErrorCode.USER_2604); // 이메일 형식이 올바르지 않습니다.
        }
        if (!isValidEmail(newEmail)) {
            throw new CustomException(ErrorCode.USER_2604);
        }
        if (currentPw == null || currentPw.isEmpty()) {
            throw new CustomException(ErrorCode.USER_2611); // 비밀번호가 일치하지 않습니다.
        }

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_2622));

        // 소셜 전용 계정은 이메일 변경 금지
        if (user.isSocialOnly()) {
            throw new CustomException(ErrorCode.USER_2615);
        }

        // 현재 비밀번호 확인
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new CustomException(ErrorCode.USER_2612); // 비번 없음(소셜/이상치)
        }
        if (!passwordEncoder.matches(currentPw, user.getPassword())) {
            throw new CustomException(ErrorCode.USER_2611);
        }

        // 동일 이메일 방지
        if (user.getEmail() != null && user.getEmail().equalsIgnoreCase(newEmail)) {
            throw new CustomException(ErrorCode.USER_2616); // 기존 이메일과 동일합니다.
        }

        // 중복 이메일 검사
        if (userRepository.existsByEmail(newEmail)) {
            throw new CustomException(ErrorCode.USER_2601); // 이미 가입된 이메일
        }

        // 변경 + 리프레시 토큰 무효화
        user.changeEmail(newEmail);
        refreshTokenRepository.deleteByUserId(user.getUserId());

        return new EmailChangeResponseDto(user.getEmail(), java.time.OffsetDateTime.now().toString());
    }

    // 비밀번호 변경
    @Transactional
    public PasswordChangeResponseDto changePassword(Long userId, PasswordChangeRequestDto request) {

        String currentPw = request.getCurrentPassword();
        String newPw = request.getNewPassword();
        String confirmPw = request.getConfirmPassword();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_2622)); // 존재하지 않는 회원

        // password가 null이면 로컬 비번 미설정 상태로 간주
        if (user.getPassword() == null || user.isSocialOnly() || user.getPassword().isBlank()) {
            throw new CustomException(ErrorCode.USER_2612); // 소셜 계정은 비밀번호 변경 불가
        }

        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(currentPw, user.getPassword())) {
            throw new CustomException(ErrorCode.USER_2611);
        }

        // 새 비밀번호와 확인 불일치
        if (!newPw.equals(confirmPw)) {
            throw new CustomException(ErrorCode.USER_2613);
        }

        // 비밀번호 형식 오류
        if (!isValidPassword(request.getNewPassword())) {
            throw new CustomException(ErrorCode.USER_2603);
        }

        // 이전 비번 재사용 방지
        if (passwordEncoder.matches(newPw, user.getPassword())) {
            throw new CustomException(ErrorCode.USER_2614);
        }

        // 저장
        user.changePassword(passwordEncoder.encode(newPw));

        // Refresh 토큰 삭제 (모든 기기 로그아웃)
        refreshTokenRepository.deleteByUserId(userId);

        return PasswordChangeResponseDto.builder()
                .userId(user.getUserId())
                .changedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 비밀번호 재설정을 위해 해당 이메일로 인증코드 전송
     */
    public void sendPasswordResetCode(String email) {
        // 1. 이메일 형식 유효성 검사
        if (email == null || !isValidEmail(email)) {
            throw new CustomException(ErrorCode.USER_2604); // 이메일 형식이 올바르지 않습니다.
        }

        // 2. 이메일로 사용자 조회
        User user = userRepository.findByEmail(email).orElse(null);

        // 3. 사용자 존재 여부와 관계없이 일관된 흐름 처리 (User Enumeration 공격 방지)
        if (user == null) {
            // 존재하지 않는 이메일이라도 에러를 발생시키지 않고, 정상적인 것처럼 응답
            // 서버 로그에만 기록하여 추적
            System.out.println("존재하지 않는 이메일로 비밀번호 재설정 요청: " + email);
            return; // 여기서 메서드를 종료하여 실제 메일 발송을 막음
        }

        // 4. 소셜 로그인 유저는 비밀번호 재설정 불가
        if (user.getLoginType() != User.LoginType.LOCAL) {
            throw new CustomException(ErrorCode.USER_2612); // 소셜 계정은 비밀번호를 변경할 수 없습니다.
        }

        // 5. 인증 코드 생성
        String verificationCode = createVerificationCode();

        // 6. 생성된 코드를  DB에 유효 시간(5분)과 함께 저장하는 로직
        verificationCodeService.saveCode(email, verificationCode);

        // 7. 이메일 발송
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("[Snack] 비밀번호 재설정 인증 코드입니다.");
            message.setText("인증 코드: " + verificationCode + "\n\n이 코드를 5분 내에 입력해주세요.");
            mailSender.send(message);
        } catch (MailException e) {
            throw new CustomException(ErrorCode.USER_2661); // 해당 이메일에 인증코드 전송을 실패했습니다.
        }
    }

    /**
     * 6자리 숫자 인증 코드를 생성하는 메서드
     */
    private String createVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 100000 ~ 999999
        return String.valueOf(code);
    }

    /**
     * 인증 코드를 검증하는 메서드
     */
    @Transactional
    public void verifyPasswordResetCode(String email, String code) {

        // 1. 이메일로 저장된 인증 코드 정보 조회
        VerificationCode verificationCode = verificationCodeRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_2671)); // 코드가 존재하지 않음

        // 2. 코드 유효 시간 확인
        if (LocalDateTime.now().isAfter(verificationCode.getExpiresAt())) {
            throw new CustomException(ErrorCode.USER_2672); // 유효 시간이 만료됨
        }

        // 3. 입력된 코드와 저장된 코드 비교
        if (!verificationCode.getCode().equals(code)) {
            throw new CustomException(ErrorCode.USER_2671); // 코드가 일치하지 않음
        }

        // 4. 인증 성공 시, 사용된 인증 코드를 DB에서 삭제
        verificationCodeRepository.delete(verificationCode);
    }

    /**
     * 인증코드 검증 후 비밀번호 재설정(비밀번호 찾기)
     */
    @Transactional
    public void resetPassword(PasswordResetDto request) {
        // 1. 이메일로 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_2622)); // 존재하지 않는 회원

        // 2. 새 비밀번호와 확인용 비밀번호가 일치하는지 확인
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new CustomException(ErrorCode.USER_2613); // 새 비밀번호 불일치
        }

        // 3. 새 비밀번호가 형식에 맞는지 확인
        if (!isValidPassword(request.getNewPassword())) {
            throw new CustomException(ErrorCode.USER_2603); // 비밀번호 형식 오류
        }

        // 4. 새로운 비밀번호를 암호화하여 저장
        user.changePassword(passwordEncoder.encode(request.getNewPassword()));
        // 모든 기기 로그아웃
        refreshTokenRepository.deleteByUserId(user.getUserId());
    }


    // 비밀번호 정규식 체크 메소드
    private boolean isValidPassword(String password) {
        // 최소 8자, 영문 1개 이상, 숫자 1개 이상, 특수문자 허용(공백 제외)
        String regex = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d!@#$%^&*()_+\\-={}\\[\\]:;\"'<>,.?/\\\\|`~]{8,}$";
        return password != null && password.matches(regex);

    }
    // 닉네임 정규식 체크 메소드
    private boolean isValidNickname(String nickname) {
        // 한글, 영문, 숫자로만 이루어진 2~6자의 문자열
        String regex = "^[가-힣a-zA-Z0-9]{2,6}$";
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
                throw new CustomException(ErrorCode.USER_2602);
            }
        }

        // 사용자 정보 업데이트
        managedUser.updateUserInfo(request.getNickname(), request.getProfileImage(), request.getIntroduction());

        return UserUpdateResponseDto.fromEntity(managedUser);
    }
}

