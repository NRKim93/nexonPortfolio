package com.portfolio.nexon.global.security.apikey;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

	private final ApiClientPrincipal principal;

	public ApiKeyAuthenticationToken(
		ApiClientPrincipal principal,
		Collection<? extends GrantedAuthority> authorities
	) {
		super(authorities);
		this.principal = principal;
		setAuthenticated(true);
	}

	@Override
	public Object getCredentials() {
		return "";
	}

	@Override
	public ApiClientPrincipal getPrincipal() {
		return principal;
	}

	@Override
	public String getName() {
		return principal.clientId();
	}
}
