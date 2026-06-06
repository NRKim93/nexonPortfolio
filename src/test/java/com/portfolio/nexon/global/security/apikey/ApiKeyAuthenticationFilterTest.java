package com.portfolio.nexon.global.security.apikey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.nexon.domain.apiclient.entity.ApiClient;
import com.portfolio.nexon.domain.apiclient.entity.ApiClientRole;
import com.portfolio.nexon.domain.apiclient.entity.ApiClientStatus;
import com.portfolio.nexon.domain.apiclient.service.ApiClientService;
import com.portfolio.nexon.global.common.error.ErrorCode;
import com.portfolio.nexon.global.exception.BusinessException;
import com.portfolio.nexon.global.security.handler.JwtAuthenticationEntryPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class ApiKeyAuthenticationFilterTest {

	private final ApiClientService apiClientService = org.mockito.Mockito.mock(ApiClientService.class);
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(
		apiClientService,
		new JwtAuthenticationEntryPoint(objectMapper)
	);

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void requestWithoutApiKeyHeadersDoesNotAuthenticate() throws Exception {
		filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verifyNoInteractions(apiClientService);
	}

	@Test
	void requestWithValidApiKeySetsAuthentication() throws Exception {
		when(apiClientService.authenticate("client-1", "api-key"))
			.thenReturn(new ApiClient("client-1", "hash", "test client", ApiClientRole.USER));
		MockHttpServletRequest request = requestWithHeaders("client-1", "api-key");

		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		assertThat(authentication.getName()).isEqualTo("client-1");
		assertThat(authentication.getAuthorities())
			.extracting("authority")
			.containsExactly("ROLE_USER");
	}

	@Test
	void requestWithAdminApiClientHasAdminAuthority() throws Exception {
		when(apiClientService.authenticate("admin-client", "api-key"))
			.thenReturn(new ApiClient("admin-client", "hash", "admin client", ApiClientRole.ADMIN));
		MockHttpServletRequest request = requestWithHeaders("admin-client", "api-key");

		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
			.extracting("authority")
			.containsExactly("ROLE_ADMIN");
	}

	@Test
	void requestWithMissingApiKeyReturnsAuthenticationFailure() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(ApiKeyAuthenticationFilter.CLIENT_ID_HEADER, "client-1");
		MockHttpServletResponse response = new MockHttpServletResponse();
		when(apiClientService.authenticate("client-1", null))
			.thenThrow(new BusinessException(ErrorCode.AUTHENTICATION_FAILED));

		filter.doFilter(request, response, new MockFilterChain());

		assertThat(response.getStatus()).isEqualTo(401);
		verify(apiClientService).authenticate("client-1", null);
	}

	@Test
	void requestWithUnknownClientReturnsAuthenticationFailure() throws Exception {
		MockHttpServletRequest request = requestWithHeaders("unknown-client", "api-key");
		MockHttpServletResponse response = new MockHttpServletResponse();
		when(apiClientService.authenticate("unknown-client", "api-key"))
			.thenThrow(new BusinessException(ErrorCode.AUTHENTICATION_FAILED));

		filter.doFilter(request, response, new MockFilterChain());

		assertThat(response.getStatus()).isEqualTo(401);
	}

	@Test
	void requestWithWrongApiKeyReturnsAuthenticationFailure() throws Exception {
		MockHttpServletRequest request = requestWithHeaders("client-1", "wrong-api-key");
		MockHttpServletResponse response = new MockHttpServletResponse();
		when(apiClientService.authenticate("client-1", "wrong-api-key"))
			.thenThrow(new BusinessException(ErrorCode.AUTHENTICATION_FAILED));

		filter.doFilter(request, response, new MockFilterChain());

		assertThat(response.getStatus()).isEqualTo(401);
	}

	@Test
	void requestWithDisabledClientReturnsAuthenticationFailure() throws Exception {
		ApiClient disabledClient = new ApiClient("client-1", "hash", "test client", ApiClientRole.USER);
		disabledClient.changeStatus(ApiClientStatus.DISABLED);
		MockHttpServletRequest request = requestWithHeaders("client-1", "api-key");
		MockHttpServletResponse response = new MockHttpServletResponse();
		when(apiClientService.authenticate("client-1", "api-key"))
			.thenThrow(new BusinessException(ErrorCode.AUTHENTICATION_FAILED));

		filter.doFilter(request, response, new MockFilterChain());

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(disabledClient.isActive()).isFalse();
	}

	private MockHttpServletRequest requestWithHeaders(String clientId, String apiKey) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(ApiKeyAuthenticationFilter.CLIENT_ID_HEADER, clientId);
		request.addHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER, apiKey);

		return request;
	}
}
