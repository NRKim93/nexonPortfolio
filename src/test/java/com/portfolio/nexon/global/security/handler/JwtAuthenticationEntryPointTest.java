package com.portfolio.nexon.global.security.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.nexon.global.common.error.ErrorCode;
import com.portfolio.nexon.global.common.response.CommonResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

class JwtAuthenticationEntryPointTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(objectMapper);

	@Test
	void authenticationFailureReturnsUnauthorizedResponse() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		entryPoint.commence(new MockHttpServletRequest(), response, new BadCredentialsException("failed"));

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(response.getContentAsString()).isEqualTo(
			objectMapper.writeValueAsString(CommonResponse.error(ErrorCode.AUTHENTICATION_FAILED))
		);
	}
}
