package umc.snack.common.exception;

public enum ErrorCode {

    // 인증/Auth
    AUTH_2101(401, "인증 정보가 누락되었습니다."),
    AUTH_2102(401, "유효하지 않은 토큰입니다."),
    AUTH_2103(419, "토큰이 만료되었습니다."),
    AUTH_2104(403, "접근 권한이 없습니다."),

    // 요청/파라미터
    REQ_3101(400, "필수 파라미터가 누락되었습니다."),
    REQ_3102(400, "파라미터 형식이 잘못되었습니다."),
    REQ_3103(415, "지원하지 않는 Content-Type입니다."),

    // API
    API_4101(404, "존재하지 않는 API입니다."),
    API_4102(405, "지원하지 않는 HTTP 메서드입니다."),

    // 서버
    SERVER_5101(500, "서버 내부 오류입니다."),
    SERVER_5102(504, "외부 서비스 응답 지연"),
    SERVER_5103(503, "서버가 일시적으로 불안정합니다.");

    private final int status;
    private final String message;

    ErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public int getStatus() { return status; }
    public String getMessage() { return message; }
}