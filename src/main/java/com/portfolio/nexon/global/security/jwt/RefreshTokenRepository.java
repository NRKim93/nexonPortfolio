package com.portfolio.nexon.global.security.jwt;

import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class RefreshTokenRepository {

	private static final String KEY_PREFIX = "refresh-token:";

	private final StringRedisTemplate redisTemplate;
	private final JwtProperties jwtProperties;

	public RefreshTokenRepository(StringRedisTemplate redisTemplate, JwtProperties jwtProperties) {
		this.redisTemplate = redisTemplate;
		this.jwtProperties = jwtProperties;
	}

	public void save(String subject, String refreshToken) {
		validateSubject(subject);
		if (!StringUtils.hasText(refreshToken)) {
			throw new IllegalArgumentException("Refresh token must not be empty");
		}

		redisTemplate.opsForValue()
			.set(key(subject), refreshToken, Duration.ofSeconds(jwtProperties.refreshTokenValidityInSeconds()));
	}

	public Optional<String> findBySubject(String subject) {
		validateSubject(subject);

		return Optional.ofNullable(redisTemplate.opsForValue().get(key(subject)));
	}

	public boolean matches(String subject, String refreshToken) {
		if (!StringUtils.hasText(refreshToken)) {
			return false;
		}

		return findBySubject(subject)
			.map(refreshToken::equals)
			.orElse(false);
	}

	public void delete(String subject) {
		validateSubject(subject);

		redisTemplate.delete(key(subject));
	}

	private String key(String subject) {
		return KEY_PREFIX + subject;
	}

	private void validateSubject(String subject) {
		if (!StringUtils.hasText(subject)) {
			throw new IllegalArgumentException("Refresh token subject must not be empty");
		}
	}
}
