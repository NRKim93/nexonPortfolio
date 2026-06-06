package com.portfolio.nexon.global.security.apikey;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiKeyHasherTest {

	private final ApiKeyHasher apiKeyHasher = new ApiKeyHasher();

	@Test
	void sameInputCreatesSameHash() {
		String first = apiKeyHasher.hash("api-key");
		String second = apiKeyHasher.hash("api-key");

		assertThat(first).isEqualTo(second);
		assertThat(first).hasSize(64);
	}

	@Test
	void differentInputCreatesDifferentHash() {
		String first = apiKeyHasher.hash("api-key");
		String second = apiKeyHasher.hash("other-api-key");

		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void matchesComparesApiKeyAndHash() {
		String hash = apiKeyHasher.hash("api-key");

		assertThat(apiKeyHasher.matches("api-key", hash)).isTrue();
		assertThat(apiKeyHasher.matches("wrong-api-key", hash)).isFalse();
	}
}
