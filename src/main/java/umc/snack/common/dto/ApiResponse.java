package umc.snack.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> { // result 필드의 타입을 유연하게 처리하기 위해 제네릭 사용
    private Boolean isSuccess; // API 호출 실행 결과 (필수)
    private String code;       // 응답 코드 (필수)
    private String message;    // 사용자에게 보여줄 메시지 (필수)
    private T result;          // API 응답 데이터 (DTO, List, null 등) - isSuccess가 true일 때 포함 (선택)
    private Object error;      // 오류 원인에 대한 설명 - isSuccess가 false일 때 포함 (선택)

    // --- 정적 팩토리 메서드: 성공 응답 생성 ---
    public static <T> ApiResponse<T> onSuccess(String code, String message, T result) {
        return ApiResponse.<T>builder()
                .isSuccess(true)
                .code(code)
                .message(message)
                .result(result)
                .error(null) // 성공 시 error는 null
                .build();
    }

    // result가 없는 성공 응답 (예: 생성, 삭제 성공 시)
    public static ApiResponse<?> onSuccess(String code, String message) {
        return ApiResponse.builder()
                .isSuccess(true)
                .code(code)
                .message(message)
                .result(null)
                .error(null)
                .build();
    }

    // --- 정적 팩토리 메서드: 실패 응답 생성 ---
    public static <T> ApiResponse<T> onFailure(String code, String message, Object error) {
        return ApiResponse.<T>builder()
                .isSuccess(false)
                .code(code)
                .message(message)
                .result(null) // 실패 시 result는 null
                .error(error) // 실패 시 error 정보 포함
                .build();
    }
}

