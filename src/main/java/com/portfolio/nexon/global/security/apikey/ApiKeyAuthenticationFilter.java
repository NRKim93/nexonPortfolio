package com.portfolio.nexon.global.security.apikey;

import com.portfolio.nexon.domain.apiclient.entity.ApiClient;
import com.portfolio.nexon.domain.apiclient.service.ApiClientService;
import com.portfolio.nexon.global.exception.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

	public static final String CLIENT_ID_HEADER = "X-Client-Id";
	public static final String API_KEY_HEADER = "X-Api-Key";

	private static final String ROLE_PREFIX = "ROLE_";

	private final ApiClientService apiClientService;
	private final AuthenticationEntryPoint authenticationEntryPoint;

	public ApiKeyAuthenticationFilter(
		ApiClientService apiClientService,
		AuthenticationEntryPoint authenticationEntryPoint
	) {
		this.apiClientService = apiClientService;
		this.authenticationEntryPoint = authenticationEntryPoint;
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		String clientId = request.getHeader(CLIENT_ID_HEADER);
		String apiKey = request.getHeader(API_KEY_HEADER);

		if (!StringUtils.hasText(clientId) && !StringUtils.hasText(apiKey)) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			ApiClient apiClient = apiClientService.authenticate(clientId, apiKey);
			SecurityContextHolder.getContext().setAuthentication(createAuthentication(apiClient));
			filterChain.doFilter(request, response);
		} catch (BusinessException exception) {
			SecurityContextHolder.clearContext();
			authenticationEntryPoint.commence(request, response, new ApiKeyAuthenticationException(exception));
		}
	}

	private ApiKeyAuthenticationToken createAuthentication(ApiClient apiClient) {
		ApiClientPrincipal principal = new ApiClientPrincipal(
			apiClient.getId(),
			apiClient.getClientId(),
			apiClient.getRole()
		);
		List<SimpleGrantedAuthority> authorities = List.of(
			new SimpleGrantedAuthority(ROLE_PREFIX + apiClient.getRole().name())
		);

		return new ApiKeyAuthenticationToken(principal, authorities);
	}
}
