package com.portfolio.nexon.global.security.apikey;

import com.portfolio.nexon.domain.apiclient.entity.ApiClientRole;

public record ApiClientPrincipal(
	Long id,
	String clientId,
	ApiClientRole role
) {
}
