package com.stockreading.common.response;

/**
 * 모든 API 응답의 공통 봉투.
 * 성공이든 실패든 같은 껍데기를 쓰기 때문에 클라이언트가 분기 없이 처리할 수 있다.
 * 항상 ok / fail 로 만든다. record 생성자는 public 이라 문법으로 막을 수는 없으므로,
 * 직접 new 하지 않는 규칙으로 "성공인데 error 가 있는" 모순 객체를 피한다.
 */
public record ApiResponse<T>(boolean success, T data, ErrorBody error) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null);
    }

    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorBody(code, message));
    }

    public record ErrorBody(String code, String message) {
    }
}
