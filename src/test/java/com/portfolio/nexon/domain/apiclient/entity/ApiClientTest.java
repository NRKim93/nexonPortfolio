package com.portfolio.nexon.domain.apiclient.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiClientTest {

	@Test
	void newApiClientStartsActive() {
		ApiClient apiClient = new ApiClient("client-1", "hash", "test client", ApiClientRole.USER);

		assertThat(apiClient.getStatus()).isEqualTo(ApiClientStatus.ACTIVE);
		assertThat(apiClient.isActive()).isTrue();
	}

	@Test
	void changeStatusUpdatesStatus() {
		ApiClient apiClient = new ApiClient("client-1", "hash", "test client", ApiClientRole.USER);

		apiClient.changeStatus(ApiClientStatus.DISABLED);

		assertThat(apiClient.getStatus()).isEqualTo(ApiClientStatus.DISABLED);
		assertThat(apiClient.isActive()).isFalse();
	}
}
