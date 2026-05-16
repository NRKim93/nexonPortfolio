package com.portfolio.nexon.global.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ErrorCodeTest {

	@Test
	void errorCodeContainsDocumentedValues() {
		assertThat(ErrorCode.AUTHENTICATION_FAILED.code()).isEqualTo("AUTH-001");
		assertThat(ErrorCode.AUTHENTICATION_FAILED.httpStatus()).isEqualTo(401);
		assertThat(ErrorCode.AUTHENTICATION_FAILED.message()).isEqualTo("AUTHENTICATION FAILED");
	}
}
