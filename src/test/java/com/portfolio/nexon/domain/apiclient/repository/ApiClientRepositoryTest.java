package com.portfolio.nexon.domain.apiclient.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.portfolio.nexon.domain.apiclient.entity.ApiClient;
import com.portfolio.nexon.domain.apiclient.entity.ApiClientRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class ApiClientRepositoryTest {

	private final ApiClientRepository apiClientRepository;

	@Autowired
	ApiClientRepositoryTest(ApiClientRepository apiClientRepository) {
		this.apiClientRepository = apiClientRepository;
	}

	@Test
	void findByClientIdReturnsApiClient() {
		ApiClient apiClient = new ApiClient("client-1", "hash", "test client", ApiClientRole.ADMIN);
		apiClientRepository.save(apiClient);

		assertThat(apiClientRepository.findByClientId("client-1"))
			.isPresent()
			.get()
			.extracting(ApiClient::getRole)
			.isEqualTo(ApiClientRole.ADMIN);
	}
}
