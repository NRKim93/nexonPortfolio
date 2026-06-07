package com.portfolio.nexon.domain.apiclient.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class ApiClientBearerAuthenticationIntegrationTest {

	private static final String TOKEN_URL = "/api/v1/auth/token";
	private static final String USER_AUTH_CHECK_URL = "/api/v1/api-clients/auth-check";
	private static final String ADMIN_AUTH_CHECK_URL = "/api/v1/admin/api-clients/auth-check";
	private static final String USER_CLIENT_ID = "user-client";
	private static final String USER_API_KEY = "user-api-key";
	private static final String ADMIN_CLIENT_ID = "admin-client";
	private static final String ADMIN_API_KEY = "admin-api-key";

	private final MockMvc mockMvc;
	private final ObjectMapper objectMapper;
	private final ApiClientRepository apiClientRepository;
	private final ApiKeyHasher apiKeyHasher;

	@Autowired
	ApiClientBearerAuthenticationIntegrationTest(
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
	void protectedApiWithoutTokenReturnsAuthenticationFailed() throws Exception {
		mockMvc.perform(get(USER_AUTH_CHECK_URL))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_FAILED.code()))
			.andExpect(jsonPath("$.message").value(ErrorCode.AUTHENTICATION_FAILED.message()))
			.andExpect(jsonPath("$.httpStatus").value(ErrorCode.AUTHENTICATION_FAILED.httpStatus()));
	}

	@Test
	void userTokenCanAccessUserProtectedApi() throws Exception {
		saveApiClient(USER_CLIENT_ID, USER_API_KEY, ApiClientRole.USER);
		String accessToken = issueToken(USER_CLIENT_ID, USER_API_KEY);

		mockMvc.perform(get(USER_AUTH_CHECK_URL)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("SUCCESS"))
			.andExpect(jsonPath("$.data.clientId").value(USER_CLIENT_ID))
			.andExpect(jsonPath("$.data.authorities[0]").value("ROLE_USER"));
	}

	@Test
	void userTokenCannotAccessAdminProtectedApi() throws Exception {
		saveApiClient(USER_CLIENT_ID, USER_API_KEY, ApiClientRole.USER);
		String accessToken = issueToken(USER_CLIENT_ID, USER_API_KEY);

		mockMvc.perform(get(ADMIN_AUTH_CHECK_URL)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.code()))
			.andExpect(jsonPath("$.message").value(ErrorCode.FORBIDDEN.message()))
			.andExpect(jsonPath("$.httpStatus").value(ErrorCode.FORBIDDEN.httpStatus()));
	}

	@Test
	void adminTokenCanAccessAdminProtectedApi() throws Exception {
		saveApiClient(ADMIN_CLIENT_ID, ADMIN_API_KEY, ApiClientRole.ADMIN);
		String accessToken = issueToken(ADMIN_CLIENT_ID, ADMIN_API_KEY);

		mockMvc.perform(get(ADMIN_AUTH_CHECK_URL)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("SUCCESS"))
			.andExpect(jsonPath("$.data.clientId").value(ADMIN_CLIENT_ID))
			.andExpect(jsonPath("$.data.authorities[0]").value("ROLE_ADMIN"));
	}

	@Test
	void tamperedTokenCannotAccessProtectedApi() throws Exception {
		saveApiClient(USER_CLIENT_ID, USER_API_KEY, ApiClientRole.USER);
		String accessToken = issueToken(USER_CLIENT_ID, USER_API_KEY);
		String tamperedToken = accessToken.substring(0, accessToken.length() - 1) + "x";

		mockMvc.perform(get(USER_AUTH_CHECK_URL)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + tamperedToken))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_FAILED.code()))
			.andExpect(jsonPath("$.message").value(ErrorCode.AUTHENTICATION_FAILED.message()))
			.andExpect(jsonPath("$.httpStatus").value(ErrorCode.AUTHENTICATION_FAILED.httpStatus()));
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

		String response = result.getResponse().getContentAsString();
		return objectMapper.readTree(response).path("data").path("accessToken").asText();
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
