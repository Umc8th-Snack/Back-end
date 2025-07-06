package umc.snack.common.exception;

public enum ErrorCode {

    // 인증/Auth
    AUTH_2001(401, "인증 정보가 누락되었습니다."),
    AUTH_2002(401, "유효하지 않은 토큰입니다."),
    AUTH_2003(419, "토큰이 만료되었습니다."),
    AUTH_2004(403, "접근 권한이 없습니다."),
    USER_2001(400, "가입되지 않은 계정입니다."),
    USER_2002(401, "로그인 실패"),

    // 요청/파라미터
    REQ_3001(400, "필수 파라미터가 누락되었습니다."),
    REQ_3002(400, "파라미터 형식이 잘못되었습니다."),
    REQ_3003(415, "지원하지 않는 Content-Type입니다."),

    // API
    API_4001(404, "존재하지 않는 API입니다."),
    API_4002(405, "지원하지 않는 HTTP 메서드입니다."),

    // 서버
    SERVER_5001(500, "서버 내부 오류입니다."),
    SERVER_5002(504, "외부 서비스 응답 지연"),
    SERVER_5003(503, "서버가 일시적으로 불안정합니다.");

    private final int status;
    private final String message;

    ErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public int getStatus() { return status; }
    public String getMessage() { return message; }
}