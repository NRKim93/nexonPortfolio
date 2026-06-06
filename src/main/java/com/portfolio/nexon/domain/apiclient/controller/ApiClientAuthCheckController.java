package com.portfolio.nexon.domain.apiclient.controller;

import com.portfolio.nexon.domain.apiclient.dto.ApiClientAuthCheckResponse;
import com.portfolio.nexon.global.common.response.CommonResponse;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("local")
public class ApiClientAuthCheckController {

	@GetMapping("/api/v1/api-clients/auth-check")
	public CommonResponse<ApiClientAuthCheckResponse> authCheck(Authentication authentication) {
		return CommonResponse.success(authenticationData(authentication));
	}

	@GetMapping("/api/v1/admin/api-clients/auth-check")
	public CommonResponse<ApiClientAuthCheckResponse> adminAuthCheck(Authentication authentication) {
		return CommonResponse.success(authenticationData(authentication));
	}

	private ApiClientAuthCheckResponse authenticationData(Authentication authentication) {
		List<String> authorities = authentication.getAuthorities().stream()
			.map(GrantedAuthority::getAuthority)
			.toList();

		return new ApiClientAuthCheckResponse(authentication.getName(), authorities);
	}
}
