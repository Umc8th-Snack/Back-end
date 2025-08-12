package umc.snack.Mock;

import jakarta.annotation.PostConstruct;
import umc.snack.domain.user.entity.User;

public class UserMock {
    public static User mock() {
        return User.builder()
                .email("mockuser@example.com")
                .password("mockpassword")
                .nickname("MockUser")
//                .profileUrl("https://example.com/mock-profile")
                .profileImage("https://example.com/mock-image")
                .introduction("This is a mock user for testing.")
                .status(User.Status.ACTIVE)
                .role(User.Role.ROLE_USER)
                .build();
    }

}