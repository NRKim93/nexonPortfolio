package com.portfolio.nexon.domain.apiclient.dto;

import java.util.List;

public record ApiClientAuthCheckResponse(
	String clientId,
	List<String> authorities
) {
}
