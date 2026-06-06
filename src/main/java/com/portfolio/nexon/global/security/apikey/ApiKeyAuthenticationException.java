package com.portfolio.nexon.global.security.apikey;

import org.springframework.security.core.AuthenticationException;

public class ApiKeyAuthenticationException extends AuthenticationException {

	public ApiKeyAuthenticationException(Throwable cause) {
		super("API key authentication failed", cause);
	}
}
