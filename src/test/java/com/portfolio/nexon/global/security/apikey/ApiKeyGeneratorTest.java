package com.portfolio.nexon.global.security.apikey;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiKeyGeneratorTest {

	private final ApiKeyGenerator apiKeyGenerator = new ApiKeyGenerator();

	@Test
	void generateReturnsUrlSafeApiKey() {
		String apiKey = apiKeyGenerator.generate();

		assertThat(apiKey).hasSizeGreaterThanOrEqualTo(40);
		assertThat(apiKey).matches("[A-Za-z0-9_-]+");
	}

	@Test
	void generateReturnsDifferentValues() {
		String first = apiKeyGenerator.generate();
		String second = apiKeyGenerator.generate();

		assertThat(first).isNotEqualTo(second);
	}
}
