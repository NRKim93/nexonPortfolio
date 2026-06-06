package com.portfolio.nexon.global.security.apikey;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ApiKeyHasher {

	private static final String SHA_256 = "SHA-256";

	public String hash(String apiKey) {
		if (!StringUtils.hasText(apiKey)) {
			throw new IllegalArgumentException("API key must not be empty");
		}

		try {
			MessageDigest messageDigest = MessageDigest.getInstance(SHA_256);
			byte[] digest = messageDigest.digest(apiKey.getBytes(StandardCharsets.UTF_8));

			return HexFormat.of().formatHex(digest);
		} catch (Exception exception) {
			throw new IllegalStateException("API key hashing failed", exception);
		}
	}

	public boolean matches(String apiKey, String apiKeyHash) {
		if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(apiKeyHash)) {
			return false;
		}

		byte[] actual = hash(apiKey).getBytes(StandardCharsets.UTF_8);
		byte[] expected = apiKeyHash.getBytes(StandardCharsets.UTF_8);

		return MessageDigest.isEqual(actual, expected);
	}
}
