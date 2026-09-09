package com.stockreading.common.error;

import org.springframework.http.HttpStatus;

/**
 * 에러 코드와 HTTP 상태를 한 곳에서 관리한다.
 * 도메인별 코드는 각 이슈에서 필요할 때 추가한다. (예: GLOSSARY_NOT_FOUND)
 */
public enum ErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청이 올바르지 않습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
