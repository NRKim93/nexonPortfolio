package com.portfolio.nexon.domain.apiclient.service;

import com.portfolio.nexon.domain.apiclient.dto.ApiClientTokenIssueRequest;
import com.portfolio.nexon.domain.apiclient.dto.ApiClientTokenIssueResponse;
import com.portfolio.nexon.domain.apiclient.entity.ApiClient;
import com.portfolio.nexon.global.security.jwt.JwtProperties;
import com.portfolio.nexon.global.security.jwt.JwtTokenProvider;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ApiClientTokenService {

	private static final String TOKEN_TYPE = "Bearer";
	private static final String ROLE_PREFIX = "ROLE_";

	private final ApiClientService apiClientService;
	private final JwtTokenProvider jwtTokenProvider;
	private final JwtProperties jwtProperties;

	public ApiClientTokenService(
		ApiClientService apiClientService,
		JwtTokenProvider jwtTokenProvider,
		JwtProperties jwtProperties
	) {
		this.apiClientService = apiClientService;
		this.jwtTokenProvider = jwtTokenProvider;
		this.jwtProperties = jwtProperties;
	}

	public ApiClientTokenIssueResponse issueToken(ApiClientTokenIssueRequest request) {
		ApiClient apiClient = apiClientService.authenticate(request.clientId(), request.apiKey());
		String role = apiClient.getRole().name();
		String accessToken = jwtTokenProvider.createAccessToken(
			apiClient.getClientId(),
			List.of(ROLE_PREFIX + role)
		);

		return new ApiClientTokenIssueResponse(
			accessToken,
			TOKEN_TYPE,
			jwtProperties.accessTokenValidityInSeconds(),
			apiClient.getClientId(),
			role
		);
	}
}
