package com.stockreading.common.error;

/**
 * "버그가 아닌 실패"를 나타내는 예외. 없는 ID 조회, 규칙 위반 등.
 * ErrorCode 를 실어 던지면 GlobalExceptionHandler 가 그대로 봉투로 변환한다.
 * 4xx 의 message 는 사용자에게 그대로 보이므로 내부 정보를 넣지 않는다.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
