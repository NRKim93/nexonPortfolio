package com.portfolio.nexon.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RefreshTokenRepositoryTest {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private final JwtProperties jwtProperties = new JwtProperties(
		"test-secret-key-for-refresh-token-repository",
		1800,
		1209600
	);

	private RefreshTokenRepository refreshTokenRepository;

	@BeforeEach
	void setUp() {
		refreshTokenRepository = new RefreshTokenRepository(redisTemplate, jwtProperties);
	}

	@Test
	void saveStoresRefreshTokenWithConfiguredTtl() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);

		refreshTokenRepository.save("account-1", "refresh-token");

		verify(valueOperations).set(
			eq("refresh-token:account-1"),
			eq("refresh-token"),
			eq(Duration.ofSeconds(1209600))
		);
	}

	@Test
	void findBySubjectReturnsStoredRefreshToken() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get("refresh-token:account-1")).thenReturn("refresh-token");

		Optional<String> result = refreshTokenRepository.findBySubject("account-1");

		assertThat(result).contains("refresh-token");
	}

	@Test
	void matchesReturnsTrueWhenStoredTokenIsEqual() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get("refresh-token:account-1")).thenReturn("refresh-token");

		boolean result = refreshTokenRepository.matches("account-1", "refresh-token");

		assertThat(result).isTrue();
	}

	@Test
	void matchesReturnsFalseWhenRefreshTokenIsDifferent() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get("refresh-token:account-1")).thenReturn("refresh-token");

		boolean result = refreshTokenRepository.matches("account-1", "other-token");

		assertThat(result).isFalse();
	}

	@Test
	void deleteRemovesStoredRefreshToken() {
		refreshTokenRepository.delete("account-1");

		verify(redisTemplate).delete("refresh-token:account-1");
	}

	@Test
	void saveRejectsBlankSubject() {
		assertThatThrownBy(() -> refreshTokenRepository.save(" ", "refresh-token"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Refresh token subject must not be empty");
	}

	@Test
	void saveRejectsBlankRefreshToken() {
		assertThatThrownBy(() -> refreshTokenRepository.save("account-1", " "))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Refresh token must not be empty");
	}
}
