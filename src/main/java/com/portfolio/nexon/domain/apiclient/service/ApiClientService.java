package com.portfolio.nexon.domain.apiclient.service;

import com.portfolio.nexon.domain.apiclient.dto.ApiClientCreateRequest;
import com.portfolio.nexon.domain.apiclient.dto.ApiClientCreateResponse;
import com.portfolio.nexon.domain.apiclient.entity.ApiClient;
import com.portfolio.nexon.domain.apiclient.entity.ApiClientRole;
import com.portfolio.nexon.domain.apiclient.entity.ApiClientStatus;
import com.portfolio.nexon.domain.apiclient.repository.ApiClientRepository;
import com.portfolio.nexon.global.common.error.ErrorCode;
import com.portfolio.nexon.global.exception.BusinessException;
import com.portfolio.nexon.global.security.apikey.ApiKeyGenerator;
import com.portfolio.nexon.global.security.apikey.ApiKeyHasher;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class ApiClientService {

	private static final List<String> DEFAULT_ALLOWED_IPS = List.of("*.*.*.*");
	private static final int CLIENT_ID_RANDOM_BYTES = 4;

	private final ApiClientRepository apiClientRepository;
	private final ApiKeyGenerator apiKeyGenerator;
	private final ApiKeyHasher apiKeyHasher;
	private final SecureRandom secureRandom = new SecureRandom();

	public ApiClientService(
		ApiClientRepository apiClientRepository,
		ApiKeyGenerator apiKeyGenerator,
		ApiKeyHasher apiKeyHasher
	) {
		this.apiClientRepository = apiClientRepository;
		this.apiKeyGenerator = apiKeyGenerator;
		this.apiKeyHasher = apiKeyHasher;
	}

	@Transactional
	public ApiClientCreateResponse create(ApiClientCreateRequest request) {
		if (apiClientRepository.existsByName(request.clientName())) {
			throw new BusinessException(ErrorCode.CLIENT_ALREADY_EXISTS);
		}

		String apiKey = apiKeyGenerator.generate();
		ApiClient apiClient = new ApiClient(
			generateClientId(request.clientName()),
			apiKeyHasher.hash(apiKey),
			request.clientName(),
			ApiClientRole.USER
		);
		apiClient.changeStatus(resolveStatus(request.status()));

		ApiClient savedApiClient = apiClientRepository.save(apiClient);

		return new ApiClientCreateResponse(
			savedApiClient.getClientId(),
			savedApiClient.getName(),
			apiKey,
			savedApiClient.getStatus(),
			DEFAULT_ALLOWED_IPS,
			savedApiClient.getCreatedAt()
		);
	}

	public ApiClient authenticate(String clientId, String apiKey) {
		if (!StringUtils.hasText(clientId) || !StringUtils.hasText(apiKey)) {
			throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED);
		}

		ApiClient apiClient = apiClientRepository.findByClientId(clientId)
			.orElseThrow(() -> new BusinessException(ErrorCode.AUTHENTICATION_FAILED));

		if (!apiClient.isActive()) {
			throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED);
		}

		if (!apiKeyHasher.matches(apiKey, apiClient.getApiKeyHash())) {
			throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED);
		}

		return apiClient;
	}

	private ApiClientStatus resolveStatus(ApiClientStatus status) {
		if (status == null) {
			return ApiClientStatus.ACTIVE;
		}

		return status;
	}

	private String generateClientId(String clientName) {
		for (int attempt = 0; attempt < 10; attempt++) {
			String clientId = slugify(clientName) + "-" + randomHex();
			if (!apiClientRepository.existsByClientId(clientId)) {
				return clientId;
			}
		}

		throw new IllegalStateException("API client id generation failed");
	}

	private String slugify(String value) {
		String slug = value.toLowerCase(Locale.ROOT)
			.replaceAll("[^a-z0-9]+", "-")
			.replaceAll("(^-|-$)", "");

		if (!StringUtils.hasText(slug)) {
			return "api-client";
		}

		return slug;
	}

	private String randomHex() {
		byte[] bytes = new byte[CLIENT_ID_RANDOM_BYTES];
		secureRandom.nextBytes(bytes);

		return HexFormat.of().formatHex(bytes);
	}
}
