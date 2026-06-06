package com.portfolio.nexon.global.config;

import com.portfolio.nexon.domain.apiclient.entity.ApiClient;
import com.portfolio.nexon.domain.apiclient.entity.ApiClientRole;
import com.portfolio.nexon.domain.apiclient.entity.ApiClientStatus;
import com.portfolio.nexon.domain.apiclient.repository.ApiClientRepository;
import com.portfolio.nexon.global.security.apikey.ApiKeyHasher;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
public class LocalApiClientInitializer {

	public static final String TEST_USER_CLIENT_ID = "test-user-client";
	public static final String TEST_USER_API_KEY = "test-user-api-key";
	public static final String TEST_ADMIN_CLIENT_ID = "test-admin-client";
	public static final String TEST_ADMIN_API_KEY = "test-admin-api-key";
	public static final String TEST_DISABLED_CLIENT_ID = "test-disabled-client";
	public static final String TEST_DISABLED_API_KEY = "test-disabled-api-key";

	@Bean
	public CommandLineRunner localApiClientData(
		ApiClientRepository apiClientRepository,
		ApiKeyHasher apiKeyHasher
	) {
		return args -> {
			createIfMissing(
				apiClientRepository,
				apiKeyHasher,
				TEST_USER_CLIENT_ID,
				TEST_USER_API_KEY,
				"Local Test User Client",
				ApiClientRole.USER,
				ApiClientStatus.ACTIVE
			);
			createIfMissing(
				apiClientRepository,
				apiKeyHasher,
				TEST_ADMIN_CLIENT_ID,
				TEST_ADMIN_API_KEY,
				"Local Test Admin Client",
				ApiClientRole.ADMIN,
				ApiClientStatus.ACTIVE
			);
			createIfMissing(
				apiClientRepository,
				apiKeyHasher,
				TEST_DISABLED_CLIENT_ID,
				TEST_DISABLED_API_KEY,
				"Local Test Disabled Client",
				ApiClientRole.USER,
				ApiClientStatus.DISABLED
			);
		};
	}

	private void createIfMissing(
		ApiClientRepository apiClientRepository,
		ApiKeyHasher apiKeyHasher,
		String clientId,
		String apiKey,
		String name,
		ApiClientRole role,
		ApiClientStatus status
	) {
		if (apiClientRepository.findByClientId(clientId).isPresent()) {
			return;
		}

		ApiClient apiClient = new ApiClient(clientId, apiKeyHasher.hash(apiKey), name, role);
		apiClient.changeStatus(status);
		apiClientRepository.save(apiClient);
	}
}
