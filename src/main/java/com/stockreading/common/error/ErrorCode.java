package com.stockreading.common.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * 에러 코드와 HTTP 상태를 한 곳에서 짝지어 관리한다.
 * 도메인별 코드(GLOSSARY_NOT_FOUND 등)는 각 이슈에서 필요할 때 여기에 추가한다.
 */
public enum ErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청이 올바르지 않습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 Content-Type 입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    /**
     * 스프링이 판단한 HTTP 상태에 맞는 코드를 고른다.
     * 전용 코드가 없는 상태는 4xx 면 INVALID_REQUEST, 그 외는 INTERNAL_ERROR 로 묶는다.
     * 이때 묶이는 것은 응답의 error.code 뿐이고 실제 HTTP 상태는 그대로 나간다.
     */
    public static ErrorCode from(HttpStatusCode status) {
        for (ErrorCode code : values()) {
            if (code.status.value() == status.value()) {
                return code;
            }
        }
        return status.is4xxClientError() ? INVALID_REQUEST : INTERNAL_ERROR;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
