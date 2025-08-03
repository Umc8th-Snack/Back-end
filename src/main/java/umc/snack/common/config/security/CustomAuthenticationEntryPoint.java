package umc.snack.common.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import umc.snack.common.dto.ApiResponse;

import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        String code = "AUTH_2113";
        String message = "유효하지 않은 인가 코드입니다.";

        if (authException instanceof AuthenticationCredentialsNotFoundException) {
            code = "AUTH_2111";
            message = "인가 코드가 전달되지 않았습니다.";
        } else if (authException.getMessage() != null && authException.getMessage().contains("expired")) {
            code = "AUTH_2112";
            message = "만료된 인가 코드입니다.";
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json; charset=UTF-8");
        ApiResponse<?> apiResponse = ApiResponse.onFailure(code, message, null);
        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
