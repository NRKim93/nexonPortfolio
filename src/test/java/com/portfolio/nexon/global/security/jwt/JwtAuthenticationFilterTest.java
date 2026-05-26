package com.portfolio.nexon.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

	private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(
		new JwtProperties("test-secret-key-for-jwt-authentication-filter", 1800),
		new ObjectMapper()
	);
	private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider);

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void requestWithoutTokenDoesNotSetAuthentication() throws Exception {
		filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void requestWithInvalidTokenClearsAuthentication() throws Exception {
		String token = jwtTokenProvider.createAccessToken("account-1", List.of("ROLE_USER"));
		String tamperedToken = token.substring(0, token.length() - 1) + "x";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer " + tamperedToken);

		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void requestWithValidTokenSetsAuthentication() throws Exception {
		String token = jwtTokenProvider.createAccessToken("account-1", List.of("ROLE_USER"));
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer " + token);

		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("account-1");
	}
}
