package com.portfolio.nexon.domain.apiclient.dto;

public record ApiClientTokenIssueResponse(
	String accessToken,
	String tokenType,
	long expiresIn,
	String clientId,
	String role
) {
}
