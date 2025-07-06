package umc.snack.common.response;

import lombok.Getter;

@Getter
public class ApiResponse<T> {
    private boolean isSuccess;
    private String code;
    private String message;
    private T result;
    private Object error;

    public ApiResponse(boolean isSuccess, String code, String message, T result, Object error) {
        this.isSuccess = isSuccess;
        this.code = code;
        this.message = message;
        this.result = result;
        this.error = error;
    }

    public static <T> ApiResponse<T> success(String code, String message, T result) {
        return new ApiResponse<>(true, code, message, result, null);
    }

    public static <T> ApiResponse<T> fail(String code, String message, Object errorDetail) {
        return new ApiResponse<>(false, code, message, null, errorDetail);
    }
}
