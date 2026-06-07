package com.portfolio.nexon.domain.apiclient.dto;

public record ApiClientApiKeyRotateResponse(
	String clientId,
	String apiKey
) {
}
