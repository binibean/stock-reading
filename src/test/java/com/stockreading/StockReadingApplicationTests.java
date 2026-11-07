package com.stockreading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartResolver;

@SpringBootTest
@AutoConfigureMockMvc
class StockReadingApplicationTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	// 실제 PostgreSQL · Redis 에 붙어 확인한다. Redis 포트가 어긋나면 여기서 잡힌다.
	// DB 설정이 틀리면 이 테스트에 오기 전에 컨텍스트 기동이 먼저 실패한다.
	@Test
	void health_는_DB_와_Redis_연결까지_UP() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.components.db.status").value("UP"))
				.andExpect(jsonPath("$.components.redis.status").value("UP"));
	}

	// 켜지면 깨진 multipart 요청이 핸들러를 찾기도 전에 500 이 된다. application.yml 참고.
	// @WebMvcTest 에는 이 자동 설정이 없어서 켜도 통과한다. 전체 컨텍스트에서 확인해야 한다.
	@Test
	void multipart_해석기는_꺼져_있다() {
		assertThat(context.getBeanNamesForType(MultipartResolver.class)).isEmpty();
	}

}
