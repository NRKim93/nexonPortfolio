package com.portfolio.nexon.domain.apiclient.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.nexon.domain.apiclient.entity.ApiClient;
import com.portfolio.nexon.domain.apiclient.entity.ApiClientRole;
import com.portfolio.nexon.domain.apiclient.entity.ApiClientStatus;
import com.portfolio.nexon.domain.apiclient.repository.ApiClientRepository;
import com.portfolio.nexon.global.common.error.ErrorCode;
import com.portfolio.nexon.global.security.apikey.ApiKeyHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiClientAdminControllerIntegrationTest {

	private static final String TOKEN_URL = "/api/v1/auth/token";
	private static final String ROTATE_URL = "/api/v1/admin/api-clients/%s/api-key/rotate";
	private static final String ADMIN_CLIENT_ID = "admin-client";
	private static final String ADMIN_API_KEY = "admin-api-key";
	private static final String TARGET_CLIENT_ID = "target-client";
	private static final String TARGET_API_KEY = "target-api-key";

	private final MockMvc mockMvc;
	private final ObjectMapper objectMapper;
	private final ApiClientRepository apiClientRepository;
	private final ApiKeyHasher apiKeyHasher;

	@Autowired
	ApiClientAdminControllerIntegrationTest(
		MockMvc mockMvc,
		ObjectMapper objectMapper,
		ApiClientRepository apiClientRepository,
		ApiKeyHasher apiKeyHasher
	) {
		this.mockMvc = mockMvc;
		this.objectMapper = objectMapper;
		this.apiClientRepository = apiClientRepository;
		this.apiKeyHasher = apiKeyHasher;
	}

	@BeforeEach
	void setUp() {
		apiClientRepository.deleteAll();
	}

	@Test
	void adminCanRotateApiKeyAndOnlyNewApiKeyCanIssueToken() throws Exception {
		saveApiClient(ADMIN_CLIENT_ID, ADMIN_API_KEY, ApiClientRole.ADMIN);
		saveApiClient(TARGET_CLIENT_ID, TARGET_API_KEY, ApiClientRole.USER);
		String adminAccessToken = issueToken(ADMIN_CLIENT_ID, ADMIN_API_KEY);

		MvcResult rotateResult = mockMvc.perform(post(ROTATE_URL.formatted(TARGET_CLIENT_ID))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("SUCCESS"))
			.andExpect(jsonPath("$.data.clientId").value(TARGET_CLIENT_ID))
			.andExpect(jsonPath("$.data.apiKey").isNotEmpty())
			.andReturn();

		String newApiKey = objectMapper.readTree(rotateResult.getResponse().getContentAsString())
			.path("data")
			.path("apiKey")
			.asText();

		ApiClient rotatedClient = apiClientRepository.findByClientId(TARGET_CLIENT_ID).orElseThrow();
		assertThat(rotatedClient.getApiKeyHash()).isNotEqualTo(newApiKey);
		assertThat(apiKeyHasher.matches(TARGET_API_KEY, rotatedClient.getApiKeyHash())).isFalse();
		assertThat(apiKeyHasher.matches(newApiKey, rotatedClient.getApiKeyHash())).isTrue();

		issueTokenExpectingAuthenticationFailed(TARGET_CLIENT_ID, TARGET_API_KEY);
		issueToken(TARGET_CLIENT_ID, newApiKey);
	}

	@Test
	void userCannotRotateApiKey() throws Exception {
		saveApiClient(TARGET_CLIENT_ID, TARGET_API_KEY, ApiClientRole.USER);
		String userAccessToken = issueToken(TARGET_CLIENT_ID, TARGET_API_KEY);

		mockMvc.perform(post(ROTATE_URL.formatted(TARGET_CLIENT_ID))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.code()))
			.andExpect(jsonPath("$.message").value(ErrorCode.FORBIDDEN.message()))
			.andExpect(jsonPath("$.httpStatus").value(ErrorCode.FORBIDDEN.httpStatus()));
	}

	private String issueToken(String clientId, String apiKey) throws Exception {
		MvcResult result = mockMvc.perform(post(TOKEN_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "clientId": "%s",
					  "apiKey": "%s"
					}
					""".formatted(clientId, apiKey)))
			.andExpect(status().isOk())
			.andReturn();

		return objectMapper.readTree(result.getResponse().getContentAsString())
			.path("data")
			.path("accessToken")
			.asText();
	}

	private void issueTokenExpectingAuthenticationFailed(String clientId, String apiKey) throws Exception {
		mockMvc.perform(post(TOKEN_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "clientId": "%s",
					  "apiKey": "%s"
					}
					""".formatted(clientId, apiKey)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_FAILED.code()))
			.andExpect(jsonPath("$.message").value(ErrorCode.AUTHENTICATION_FAILED.message()))
			.andExpect(jsonPath("$.httpStatus").value(ErrorCode.AUTHENTICATION_FAILED.httpStatus()));
	}

	private void saveApiClient(String clientId, String apiKey, ApiClientRole role) {
		ApiClient apiClient = new ApiClient(
			clientId,
			apiKeyHasher.hash(apiKey),
			clientId + " name",
			role
		);
		apiClient.changeStatus(ApiClientStatus.ACTIVE);
		apiClientRepository.save(apiClient);
	}
}
