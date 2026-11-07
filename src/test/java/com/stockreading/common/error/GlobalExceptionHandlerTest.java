package com.stockreading.common.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stockreading.common.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.apache.tomcat.util.http.InvalidParameterException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 어떤 실패든 공통 봉투로 나가고, HTTP 상태코드와 success 가 어긋나지 않는지 검증한다.
 * 실제 도메인 API 가 아직 없으므로 실패 상황을 만드는 전용 컨트롤러를 테스트 안에 둔다.
 */
// 컨트롤러를 지정해야 한다. 비워 두면 실제 컨트롤러까지 올라와 서비스 빈이 없어 전부 실패한다.
@WebMvcTest(GlobalExceptionHandlerTest.ProbeController.class)
@ExtendWith(OutputCaptureExtension.class)
@Import(GlobalExceptionHandlerTest.ProbeController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 성공은_data_를_담은_봉투로_나간다() throws Exception {
        mockMvc.perform(get("/probe/ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("hello"))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
    void 없는_경로는_404_봉투() throws Exception {
        expectFailure(mockMvc.perform(get("/does-not-exist")), 404, "NOT_FOUND")
                .andExpect(fixedMessage(ErrorCode.NOT_FOUND));
    }

    @Test
    void BusinessException_은_실어_보낸_코드대로() throws Exception {
        expectFailure(mockMvc.perform(get("/probe/business")), 404, "NOT_FOUND")
                .andExpect(jsonPath("$.error.message").value("그런 용어는 없습니다."));
    }

    @Test
    void 지원하지_않는_메서드는_405_봉투이고_Allow_헤더를_유지한다() throws Exception {
        expectFailure(mockMvc.perform(post("/probe/ok")), 405, "METHOD_NOT_ALLOWED")
                .andExpect(fixedMessage(ErrorCode.METHOD_NOT_ALLOWED))
                .andExpect(header().string("Allow", containsString("GET")));
    }

    @Test
    void 지원하지_않는_ContentType_은_415_봉투() throws Exception {
        expectFailure(mockMvc.perform(post("/probe/valid")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("name")),
                415, "UNSUPPORTED_MEDIA_TYPE")
                .andExpect(fixedMessage(ErrorCode.UNSUPPORTED_MEDIA_TYPE));
    }

    @Test
    void Valid_검증_실패는_400_이고_틀린_필드를_알려준다() throws Exception {
        expectFailure(mockMvc.perform(post("/probe/valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}")),
                400, "INVALID_REQUEST")
                .andExpect(jsonPath("$.error.message").value(startsWith("name: ")));
    }

    @Test
    void 깨진_JSON_은_400_봉투() throws Exception {
        expectFailure(mockMvc.perform(post("/probe/valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{broken")),
                400, "INVALID_REQUEST")
                .andExpect(fixedMessage(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void 경로변수_타입이_틀리면_400_봉투() throws Exception {
        expectFailure(mockMvc.perform(get("/probe/number/abc")), 400, "INVALID_REQUEST")
                .andExpect(fixedMessage(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void 필수_쿼리파라미터가_없으면_400_봉투() throws Exception {
        expectFailure(mockMvc.perform(get("/probe/param")), 400, "INVALID_REQUEST")
                .andExpect(fixedMessage(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void Accept_가_JSON_이_아니어도_에러는_JSON_봉투로_나간다() throws Exception {
        expectFailure(mockMvc.perform(get("/does-not-exist").accept(MediaType.TEXT_HTML)), 404, "NOT_FOUND");
        expectFailure(mockMvc.perform(get("/probe/business").accept(MediaType.APPLICATION_XML)), 404, "NOT_FOUND");
        expectFailure(mockMvc.perform(get("/probe/boom").accept(MediaType.TEXT_HTML)), 500, "INTERNAL_ERROR");
    }

    @Test
    void 예상하지_못한_예외는_500_이고_내부_메시지는_로그로만_남긴다(CapturedOutput output) throws Exception {
        expectFailure(mockMvc.perform(get("/probe/boom")), 500, "INTERNAL_ERROR")
                .andExpect(fixedMessage(ErrorCode.INTERNAL_ERROR));
        assertThat(output).contains("db password is hunter2");
    }

    @Test
    void 깨진_쿼리스트링은_400_이고_ERROR_로그를_남기지_않는다(CapturedOutput output) throws Exception {
        // ?q=100% 를 해석할 때 Tomcat 이 던지는 예외. MockMvc 는 Tomcat 을 거치지 않아 직접 던진다.
        expectFailure(mockMvc.perform(get("/probe/bad-query")), 400, "INVALID_REQUEST")
                .andExpect(fixedMessage(ErrorCode.INVALID_REQUEST));
        assertThat(output).doesNotContain("처리되지 않은 예외");
    }

    @Test
    void 깨진_폼_본문은_필터에서_터지지_않고_일반_404_가_된다() throws Exception {
        // FormContentFilter 를 켜면 이 요청이 컨트롤러에 닿기 전에 실패한다. application.yml 참고.
        expectFailure(mockMvc.perform(put("/does-not-exist")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content("a=%zz")),
                404, "NOT_FOUND");
    }

    @Test
    void 바인딩_실패는_필드는_알려주되_자바_내부_메시지는_숨긴다() throws Exception {
        expectFailure(mockMvc.perform(get("/probe/filter").param("page", "abc")), 400, "INVALID_REQUEST")
                .andExpect(jsonPath("$.error.message").value(startsWith("page: ")))
                .andExpect(jsonPath("$.error.message").value(not(containsString("java.lang"))));
    }

    @Test
    void 전용_코드가_없는_4xx_는_상태를_유지하고_코드만_묶는다() throws Exception {
        expectFailure(mockMvc.perform(get("/probe/conflict")), 409, "INVALID_REQUEST")
                .andExpect(fixedMessage(ErrorCode.INVALID_REQUEST));
        expectFailure(mockMvc.perform(get("/probe/ok").accept(MediaType.APPLICATION_XML)), 406, "INVALID_REQUEST");
    }

    @Test
    void 전용_코드가_없는_5xx_는_상태를_유지하고_로그를_남긴다(CapturedOutput output) throws Exception {
        expectFailure(mockMvc.perform(get("/probe/unavailable")), 503, "INTERNAL_ERROR")
                .andExpect(fixedMessage(ErrorCode.INTERNAL_ERROR));
        assertThat(output).contains("스프링 MVC 처리 중 서버 오류");
    }

    @Test
    void 서버_오류로_분류된_BusinessException_은_메시지를_숨기고_로그로만_남긴다(CapturedOutput output) throws Exception {
        expectFailure(mockMvc.perform(get("/probe/business-500")), 500, "INTERNAL_ERROR")
                .andExpect(fixedMessage(ErrorCode.INTERNAL_ERROR));
        assertThat(output).contains("서버 오류로 분류된 BusinessException", "외부 API 호출 실패");
    }

    private ResultMatcher fixedMessage(ErrorCode code) {
        return jsonPath("$.error.message").value(code.message());
    }

    private ResultActions expectFailure(ResultActions result, int httpStatus, String code) throws Exception {
        return result
                .andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value(code));
    }

    @RestController
    static class ProbeController {

        record Body(@NotBlank String name) {
        }

        record Filter(Integer page) {
        }

        @GetMapping("/probe/ok")
        ApiResponse<String> ok() {
            return ApiResponse.ok("hello");
        }

        @GetMapping("/probe/business")
        ApiResponse<Void> business() {
            throw new BusinessException(ErrorCode.NOT_FOUND, "그런 용어는 없습니다.");
        }

        @PostMapping("/probe/valid")
        ApiResponse<Void> valid(@Valid @RequestBody Body body) {
            return ApiResponse.ok();
        }

        @GetMapping("/probe/number/{id}")
        ApiResponse<Long> number(@PathVariable Long id) {
            return ApiResponse.ok(id);
        }

        @GetMapping("/probe/param")
        ApiResponse<String> param(@RequestParam String required) {
            return ApiResponse.ok(required);
        }

        @GetMapping("/probe/filter")
        ApiResponse<Void> filter(@Valid @ModelAttribute Filter filter) {
            return ApiResponse.ok();
        }

        @GetMapping("/probe/conflict")
        ApiResponse<Void> conflict() {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "내부 사정이 담긴 사유");
        }

        @GetMapping("/probe/unavailable")
        ApiResponse<Void> unavailable() {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        }

        @GetMapping("/probe/business-500")
        ApiResponse<Void> business500() {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "외부 API 호출 실패");
        }

        @GetMapping("/probe/bad-query")
        ApiResponse<Void> badQuery() {
            throw new InvalidParameterException("Invalid % sequence");
        }

        @GetMapping("/probe/boom")
        ApiResponse<Void> boom() {
            throw new IllegalStateException("db password is hunter2");
        }
    }
}
