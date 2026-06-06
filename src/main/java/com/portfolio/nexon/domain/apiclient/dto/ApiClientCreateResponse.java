package com.portfolio.nexon.domain.apiclient.dto;

import com.portfolio.nexon.domain.apiclient.entity.ApiClientStatus;
import java.time.LocalDateTime;
import java.util.List;

public record ApiClientCreateResponse(
	String clientId,
	String clientName,
	String apiKey,
	ApiClientStatus status,
	List<String> allowedIps,
	LocalDateTime createdAt
) {
}
