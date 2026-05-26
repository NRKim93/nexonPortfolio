package com.portfolio.nexon.global.security.jwt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtTokenProvider {

	private static final String HMAC_SHA256 = "HmacSHA256";
	private static final String ALG = "HS256";
	private static final String TYP = "JWT";
	private static final String ROLES = "roles";

	private final JwtProperties jwtProperties;
	private final ObjectMapper objectMapper;

	public JwtTokenProvider(JwtProperties jwtProperties, ObjectMapper objectMapper) {
		this.jwtProperties = jwtProperties;
		this.objectMapper = objectMapper;
	}

	public String createAccessToken(String subject, List<String> roles) {
		return createToken(subject, roles, jwtProperties.accessTokenValidityInSeconds());
	}

	public boolean validateToken(String token) {
		try {
			Map<String, Object> payload = parsePayload(token);
			Object expiresAt = payload.get("exp");

			return expiresAt instanceof Number number && number.longValue() > Instant.now().getEpochSecond();
		} catch (RuntimeException exception) {
			return false;
		}
	}

	public Authentication getAuthentication(String token) {
		Map<String, Object> payload = parsePayload(token);
		String subject = String.valueOf(payload.get("sub"));
		List<SimpleGrantedAuthority> authorities = extractRoles(payload).stream()
			.map(SimpleGrantedAuthority::new)
			.toList();

		return new UsernamePasswordAuthenticationToken(subject, token, authorities);
	}

	private String createToken(String subject, List<String> roles, long validityInSeconds) {
		if (!StringUtils.hasText(subject)) {
			throw new IllegalArgumentException("JWT subject must not be empty");
		}

		long now = Instant.now().getEpochSecond();
		Map<String, Object> header = new LinkedHashMap<>();
		header.put("alg", ALG);
		header.put("typ", TYP);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("sub", subject);
		payload.put(ROLES, roles);
		payload.put("iat", now);
		payload.put("exp", now + validityInSeconds);

		String unsignedToken = encodeJson(header) + "." + encodeJson(payload);

		return unsignedToken + "." + sign(unsignedToken);
	}

	private Map<String, Object> parsePayload(String token) {
		String[] parts = token.split("\\.");

		if (parts.length != 3) {
			throw new IllegalArgumentException("Invalid JWT format");
		}

		String unsignedToken = parts[0] + "." + parts[1];
		String signature = sign(unsignedToken);

		if (!signature.equals(parts[2])) {
			throw new IllegalArgumentException("Invalid JWT signature");
		}

		try {
			byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
			return objectMapper.readValue(payloadBytes, new TypeReference<>() {
			});
		} catch (Exception exception) {
			throw new IllegalArgumentException("Invalid JWT payload", exception);
		}
	}

	private List<String> extractRoles(Map<String, Object> payload) {
		Object roles = payload.get(ROLES);

		if (!(roles instanceof List<?> values)) {
			return List.of();
		}

		List<String> result = new ArrayList<>();
		for (Object value : values) {
			result.add(String.valueOf(value));
		}

		return result;
	}

	private String encodeJson(Map<String, Object> value) {
		try {
			return Base64.getUrlEncoder()
				.withoutPadding()
				.encodeToString(objectMapper.writeValueAsBytes(value));
		} catch (Exception exception) {
			throw new IllegalArgumentException("JWT encoding failed", exception);
		}
	}

	private String sign(String value) {
		try {
			Mac mac = Mac.getInstance(HMAC_SHA256);
			SecretKeySpec key = new SecretKeySpec(jwtProperties.secret().getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
			mac.init(key);

			return Base64.getUrlEncoder()
				.withoutPadding()
				.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception exception) {
			throw new IllegalArgumentException("JWT signing failed", exception);
		}
	}
}
