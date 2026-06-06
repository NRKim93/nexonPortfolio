package com.portfolio.nexon.domain.apiclient.dto;

import com.portfolio.nexon.domain.apiclient.entity.ApiClientStatus;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ApiClientCreateRequest(
	@NotBlank
	String clientName,
	String description,
	List<String> allowedIps,
	ApiClientStatus status
) {
}
