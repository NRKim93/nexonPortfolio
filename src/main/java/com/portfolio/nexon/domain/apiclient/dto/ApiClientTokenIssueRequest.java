package com.portfolio.nexon.domain.apiclient.dto;

import jakarta.validation.constraints.NotBlank;

public record ApiClientTokenIssueRequest(
	@NotBlank
	String clientId,
	@NotBlank
	String apiKey
) {
}
