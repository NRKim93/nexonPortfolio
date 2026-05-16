package com.portfolio.nexon.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.portfolio.nexon.global.common.error.ErrorCode;
import org.junit.jupiter.api.Test;

class BusinessExceptionTest {

	@Test
	void businessExceptionKeepsErrorCode() {
		BusinessException exception = new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);

		assertThat(exception.errorCode()).isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);
		assertThat(exception.getMessage()).isEqualTo("ACCOUNT NOT FOUND");
	}
}
