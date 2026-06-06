package com.portfolio.nexon.domain.apiclient.controller;

import com.portfolio.nexon.domain.apiclient.dto.ApiClientTokenIssueRequest;
import com.portfolio.nexon.domain.apiclient.dto.ApiClientTokenIssueResponse;
import com.portfolio.nexon.domain.apiclient.service.ApiClientTokenService;
import com.portfolio.nexon.global.common.response.CommonResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiClientAuthController {

	private final ApiClientTokenService apiClientTokenService;

	public ApiClientAuthController(ApiClientTokenService apiClientTokenService) {
		this.apiClientTokenService = apiClientTokenService;
	}

	@PostMapping("/api/v1/auth/token")
	public CommonResponse<ApiClientTokenIssueResponse> issueToken(
		@Valid @RequestBody ApiClientTokenIssueRequest request
	) {
		return CommonResponse.success(apiClientTokenService.issueToken(request));
	}
}
