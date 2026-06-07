package com.portfolio.nexon.domain.apiclient.controller;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiClientAuthControllerIntegrationTest {

	private static final String TOKEN_URL = "/api/v1/auth/token";
	private static final String CLIENT_ID = "client-1";
	private static final String API_KEY = "api-key";

	private final MockMvc mockMvc;
	private final ApiClientRepository apiClientRepository;
	private final ApiKeyHasher apiKeyHasher;

	@Autowired
	ApiClientAuthControllerIntegrationTest(
		MockMvc mockMvc,
		ApiClientRepository apiClientRepository,
		ApiKeyHasher apiKeyHasher
	) {
		this.mockMvc = mockMvc;
		this.apiClientRepository = apiClientRepository;
		this.apiKeyHasher = apiKeyHasher;
	}

	@BeforeEach
	void setUp() {
		apiClientRepository.deleteAll();
	}

	@Test
	void issueTokenReturnsBearerAccessToken() throws Exception {
		saveApiClient(CLIENT_ID, API_KEY, ApiClientRole.USER, ApiClientStatus.ACTIVE);

		mockMvc.perform(post(TOKEN_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "clientId": "client-1",
					  "apiKey": "api-key"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("SUCCESS"))
			.andExpect(jsonPath("$.data.accessToken", not(blankOrNullString())))
			.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.data.expiresIn").isNumber())
			.andExpect(jsonPath("$.data.clientId").value(CLIENT_ID))
			.andExpect(jsonPath("$.data.role").value("USER"));
	}

	@Test
	void issueTokenWithInvalidApiKeyReturnsAuthenticationFailed() throws Exception {
		saveApiClient(CLIENT_ID, API_KEY, ApiClientRole.USER, ApiClientStatus.ACTIVE);

		mockMvc.perform(post(TOKEN_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "clientId": "client-1",
					  "apiKey": "wrong-api-key"
					}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_FAILED.code()))
			.andExpect(jsonPath("$.message").value(ErrorCode.AUTHENTICATION_FAILED.message()))
			.andExpect(jsonPath("$.httpStatus").value(ErrorCode.AUTHENTICATION_FAILED.httpStatus()));
	}

	@Test
	void issueTokenWithUnknownClientReturnsAuthenticationFailed() throws Exception {
		mockMvc.perform(post(TOKEN_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "clientId": "unknown-client",
					  "apiKey": "api-key"
					}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_FAILED.code()))
			.andExpect(jsonPath("$.message").value(ErrorCode.AUTHENTICATION_FAILED.message()))
			.andExpect(jsonPath("$.httpStatus").value(ErrorCode.AUTHENTICATION_FAILED.httpStatus()));
	}

	@Test
	void issueTokenWithDisabledClientReturnsAuthenticationFailed() throws Exception {
		saveApiClient(CLIENT_ID, API_KEY, ApiClientRole.USER, ApiClientStatus.DISABLED);

		mockMvc.perform(post(TOKEN_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "clientId": "client-1",
					  "apiKey": "api-key"
					}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_FAILED.code()))
			.andExpect(jsonPath("$.message").value(ErrorCode.AUTHENTICATION_FAILED.message()))
			.andExpect(jsonPath("$.httpStatus").value(ErrorCode.AUTHENTICATION_FAILED.httpStatus()));
	}

	@Test
	void issueTokenWithBlankRequestReturnsBadRequest() throws Exception {
		mockMvc.perform(post(TOKEN_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "clientId": "",
					  "apiKey": ""
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(ErrorCode.BAD_REQUEST.code()))
			.andExpect(jsonPath("$.message").value(ErrorCode.BAD_REQUEST.message()))
			.andExpect(jsonPath("$.httpStatus").value(ErrorCode.BAD_REQUEST.httpStatus()));
	}

	private void saveApiClient(
		String clientId,
		String apiKey,
		ApiClientRole role,
		ApiClientStatus status
	) {
		ApiClient apiClient = new ApiClient(
			clientId,
			apiKeyHasher.hash(apiKey),
			clientId + " name",
			role
		);
		apiClient.changeStatus(status);
		apiClientRepository.save(apiClient);
	}
}
