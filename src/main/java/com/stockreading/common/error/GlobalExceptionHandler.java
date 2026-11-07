package com.stockreading.common.error;

import com.stockreading.common.response.ApiResponse;
import org.apache.tomcat.util.http.InvalidParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 컨트롤러에서 새어 나온 모든 예외를 공통 봉투로 변환한다.
 * HTTP 상태코드와 success 를 이 클래스 한 곳에서만 함께 설정하므로 둘이 어긋날 수 없다.
 *
 * ResponseEntityExceptionHandler 는 405 · 415 · 깨진 JSON · 타입 오류처럼 스프링 MVC 가
 * 던지는 표준 예외를 올바른 상태코드로 분류해 준다. 상속하지 않으면 이 예외들이 전부
 * 아래 Exception 핸들러로 떨어져 클라이언트 실수가 500 이 된다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 도메인에서 의도적으로 던진 예외 → 실어 보낸 ErrorCode 대로. 5xx 의 메시지는 로그로만 남긴다. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        ErrorCode code = e.errorCode();
        if (code.status().is5xxServerError()) {
            log.error("서버 오류로 분류된 BusinessException", e);
            return respond(code, code.message());
        }
        return respond(code, e.getMessage());
    }

    /**
     * ?q=100% 처럼 인코딩이 깨진 파라미터. Tomcat 이 파라미터를 읽는 순간 던지며
     * 스프링 표준 예외가 아니라서 잡아 주지 않으면 클라이언트 실수가 500 이 된다.
     */
    @ExceptionHandler(InvalidParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidParameter(InvalidParameterException e) {
        return respond(ErrorCode.INVALID_REQUEST, ErrorCode.INVALID_REQUEST.message());
    }

    /** 예상하지 못한 예외. 내부 메시지는 응답에 싣지 않고 로그로만 남긴다. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("처리되지 않은 예외", e);
        return respond(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.message());
    }

    /**
     * 스프링 MVC 표준 예외가 모두 거쳐 가는 지점. 부모가 만든 기본 본문 대신 봉투를 넣는다.
     * headers 를 이어받아야 405 의 Allow 같은 헤더가 유지된다.
     * HTTP 상태는 스프링이 판단한 값을 그대로 쓴다. 전용 코드가 없어 error.code 가 묶여도
     * 409 · 503 같은 실제 상태까지 바뀌면 안 된다.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ErrorCode code = ErrorCode.from(statusCode);
        if (statusCode.is5xxServerError()) {
            log.error("스프링 MVC 처리 중 서버 오류", ex);
        }
        ApiResponse<Void> envelope = ApiResponse.fail(code.name(), messageOf(ex, code));
        HttpHeaders jsonHeaders = new HttpHeaders(headers);
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        return super.handleExceptionInternal(ex, envelope, jsonHeaders, statusCode, request);
    }

    /**
     * Content-Type 을 JSON 으로 고정한다. 비워 두면 스프링이 요청의 Accept 에 맞추려다
     * Accept: text/html 같은 요청에서 봉투를 쓰지 못하고 기본 에러 페이지로 빠진다.
     */
    private ResponseEntity<ApiResponse<Void>> respond(ErrorCode code, String message) {
        return ResponseEntity.status(code.status())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.fail(code.name(), message));
    }

    /**
     * @Valid 객체 검증 실패는 틀린 필드와 이유를 알려준다. 그 외는 코드의 고정 메시지.
     * 타입 변환 실패의 기본 메시지에는 자바 타입 이름이 들어 있어 응답에 싣지 않는다.
     */
    private String messageOf(Exception ex, ErrorCode code) {
        if (ex instanceof MethodArgumentNotValidException invalid) {
            return invalid.getBindingResult().getFieldErrors().stream()
                    .findFirst()
                    .map(fe -> fe.getField() + ": "
                            + (fe.isBindingFailure() ? "값의 형식이 올바르지 않습니다." : fe.getDefaultMessage()))
                    .orElse(code.message());
        }
        return code.message();
    }
}
