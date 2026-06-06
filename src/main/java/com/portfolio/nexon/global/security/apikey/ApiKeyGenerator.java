package com.portfolio.nexon.global.security.apikey;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyGenerator {

	private static final int API_KEY_BYTES = 32;

	private final SecureRandom secureRandom = new SecureRandom();

	public String generate() {
		byte[] bytes = new byte[API_KEY_BYTES];
		secureRandom.nextBytes(bytes);

		return Base64.getUrlEncoder()
			.withoutPadding()
			.encodeToString(bytes);
	}
}
