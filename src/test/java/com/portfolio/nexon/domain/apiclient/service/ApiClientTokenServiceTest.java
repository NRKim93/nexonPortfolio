package com.portfolio.nexon.domain.apiclient.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.nexon.domain.apiclient.dto.ApiClientTokenIssueRequest;
import com.portfolio.nexon.domain.apiclient.dto.ApiClientTokenIssueResponse;
import com.portfolio.nexon.domain.apiclient.entity.ApiClient;
import com.portfolio.nexon.domain.apiclient.entity.ApiClientRole;
import com.portfolio.nexon.global.security.jwt.JwtProperties;
import com.portfolio.nexon.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class ApiClientTokenServiceTest {

	private final ApiClientService apiClientService = mock(ApiClientService.class);
	private final JwtProperties jwtProperties = new JwtProperties("test-secret-key-for-api-client-token", 3600);
	private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(jwtProperties, new ObjectMapper());
	private final ApiClientTokenService apiClientTokenService = new ApiClientTokenService(
		apiClientService,
		jwtTokenProvider,
		jwtProperties
	);

	@Test
	void issueTokenAuthenticatesApiClientAndReturnsBearerAccessToken() {
		when(apiClientService.authenticate("client-1", "api-key"))
			.thenReturn(new ApiClient("client-1", "hash", "test client", ApiClientRole.USER));
		ApiClientTokenIssueRequest request = new ApiClientTokenIssueRequest("client-1", "api-key");

		ApiClientTokenIssueResponse response = apiClientTokenService.issueToken(request);

		Authentication authentication = jwtTokenProvider.getAuthentication(response.accessToken());
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.expiresIn()).isEqualTo(3600);
		assertThat(response.clientId()).isEqualTo("client-1");
		assertThat(response.role()).isEqualTo("USER");
		assertThat(jwtTokenProvider.validateToken(response.accessToken())).isTrue();
		assertThat(authentication.getName()).isEqualTo("client-1");
		assertThat(authentication.getAuthorities())
			.extracting("authority")
			.containsExactly("ROLE_USER");
	}
}
