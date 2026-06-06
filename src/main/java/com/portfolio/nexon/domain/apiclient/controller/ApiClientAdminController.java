package com.portfolio.nexon.domain.apiclient.controller;

import com.portfolio.nexon.domain.apiclient.dto.ApiClientCreateRequest;
import com.portfolio.nexon.domain.apiclient.dto.ApiClientCreateResponse;
import com.portfolio.nexon.domain.apiclient.service.ApiClientService;
import com.portfolio.nexon.global.common.response.CommonResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiClientAdminController {

	private final ApiClientService apiClientService;

	public ApiClientAdminController(ApiClientService apiClientService) {
		this.apiClientService = apiClientService;
	}

	@PostMapping("/api/v1/admin/api-clients")
	public CommonResponse<ApiClientCreateResponse> create(@Valid @RequestBody ApiClientCreateRequest request) {
		return CommonResponse.success(apiClientService.create(request));
	}
}
