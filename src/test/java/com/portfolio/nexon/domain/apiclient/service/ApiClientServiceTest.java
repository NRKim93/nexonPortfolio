package com.portfolio.nexon.domain.apiclient.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ApiClientServiceTest {

	private final ApiClientRepository apiClientRepository = mock(ApiClientRepository.class);
	private final ApiKeyGenerator apiKeyGenerator = mock(ApiKeyGenerator.class);
	private final ApiKeyHasher apiKeyHasher = new ApiKeyHasher();
	private final ApiClientService apiClientService = new ApiClientService(
		apiClientRepository,
		apiKeyGenerator,
		apiKeyHasher
	);

	@Test
	void createReturnsPlainApiKeyOnceAndStoresOnlyHash() {
		when(apiClientRepository.existsByName("game-service-a")).thenReturn(false);
		when(apiClientRepository.existsByClientId(any())).thenReturn(false);
		when(apiKeyGenerator.generate()).thenReturn("issued-api-key");
		ArgumentCaptor<ApiClient> captor = ArgumentCaptor.forClass(ApiClient.class);
		when(apiClientRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
		ApiClientCreateRequest request = new ApiClientCreateRequest(
			"game-service-a",
			"A game service client",
			null,
			null
		);

		ApiClientCreateResponse response = apiClientService.create(request);

		ApiClient savedApiClient = captor.getValue();
		assertThat(response.clientId()).startsWith("game-service-a-");
		assertThat(response.clientName()).isEqualTo("game-service-a");
		assertThat(response.apiKey()).isEqualTo("issued-api-key");
		assertThat(response.status()).isEqualTo(ApiClientStatus.ACTIVE);
		assertThat(response.allowedIps()).containsExactly("*.*.*.*");
		assertThat(savedApiClient.getApiKeyHash()).isNotEqualTo("issued-api-key");
		assertThat(apiKeyHasher.matches("issued-api-key", savedApiClient.getApiKeyHash())).isTrue();
		assertThat(savedApiClient.getRole()).isEqualTo(ApiClientRole.USER);
	}

	@Test
	void createUsesRequestedStatus() {
		when(apiClientRepository.existsByName("game-service-a")).thenReturn(false);
		when(apiClientRepository.existsByClientId(any())).thenReturn(false);
		when(apiKeyGenerator.generate()).thenReturn("issued-api-key");
		when(apiClientRepository.save(any(ApiClient.class))).thenAnswer(invocation -> invocation.getArgument(0));
		ApiClientCreateRequest request = new ApiClientCreateRequest(
			"game-service-a",
			null,
			null,
			ApiClientStatus.DISABLED
		);

		ApiClientCreateResponse response = apiClientService.create(request);

		assertThat(response.status()).isEqualTo(ApiClientStatus.DISABLED);
	}

	@Test
	void createRejectsDuplicateClientName() {
		when(apiClientRepository.existsByName("game-service-a")).thenReturn(true);
		ApiClientCreateRequest request = new ApiClientCreateRequest(
			"game-service-a",
			null,
			null,
			null
		);

		assertThatThrownBy(() -> apiClientService.create(request))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.CLIENT_ALREADY_EXISTS);
	}
}
