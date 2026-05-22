package com.portfolio.nexon.global.security.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.nexon.global.common.error.ErrorCode;
import com.portfolio.nexon.global.common.response.CommonResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

class JwtAccessDeniedHandlerTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final JwtAccessDeniedHandler handler = new JwtAccessDeniedHandler(objectMapper);

	@Test
	void accessDeniedReturnsForbiddenResponse() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		handler.handle(new MockHttpServletRequest(), response, new AccessDeniedException("denied"));

		assertThat(response.getStatus()).isEqualTo(403);
		assertThat(response.getContentAsString()).isEqualTo(
			objectMapper.writeValueAsString(CommonResponse.error(ErrorCode.FORBIDDEN))
		);
	}
}
