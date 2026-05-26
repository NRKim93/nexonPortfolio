package com.portfolio.nexon.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class JwtTokenProviderTest {

	private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(
		new JwtProperties("test-secret-key-for-jwt-token-provider", 1800),
		new ObjectMapper()
	);

	@Test
	void createdAccessTokenCanBeValidatedAndConvertedToAuthentication() {
		String token = jwtTokenProvider.createAccessToken("account-1", List.of("ROLE_USER"));

		Authentication authentication = jwtTokenProvider.getAuthentication(token);

		assertThat(jwtTokenProvider.validateToken(token)).isTrue();
		assertThat(authentication.getName()).isEqualTo("account-1");
		assertThat(authentication.getAuthorities())
			.extracting("authority")
			.containsExactly("ROLE_USER");
	}

	@Test
	void tamperedTokenIsInvalid() {
		String token = jwtTokenProvider.createAccessToken("account-1", List.of("ROLE_USER"));
		String tamperedToken = token.substring(0, token.length() - 1) + "x";

		assertThat(jwtTokenProvider.validateToken(tamperedToken)).isFalse();
	}
}
