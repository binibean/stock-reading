package com.stockreading.common.error;

import com.stockreading.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 모든 예외를 공통 응답 봉투로 변환한다.
 * 컨트롤러는 예외만 던지면 되고, 응답 형태를 신경 쓰지 않는다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 도메인에서 의도적으로 던진 예외 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        ErrorCode code = e.errorCode();
        return ResponseEntity.status(code.status())
                .body(ApiResponse.fail(code.name(), e.getMessage()));
    }

    /** 매핑되지 않은 경로 (정적 리소스 · 핸들러 모두) */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFound(Exception e) {
        ErrorCode code = ErrorCode.NOT_FOUND;
        return ResponseEntity.status(code.status())
                .body(ApiResponse.fail(code.name(), code.message()));
    }

    /** @Valid 검증 실패 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse(ErrorCode.INVALID_REQUEST.message());
        ErrorCode code = ErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(code.status())
                .body(ApiResponse.fail(code.name(), message));
    }

    /** 예상하지 못한 예외. 내부 메시지는 숨기고 로그로만 남긴다. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("처리되지 않은 예외", e);
        ErrorCode code = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(code.status())
                .body(ApiResponse.fail(code.name(), code.message()));
    }
}
