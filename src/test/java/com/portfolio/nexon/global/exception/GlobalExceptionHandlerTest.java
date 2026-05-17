package com.portfolio.nexon.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.portfolio.nexon.global.common.error.ErrorCode;
import com.portfolio.nexon.global.common.response.CommonResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void businessExceptionReturnsMappedErrorResponse() {
		ResponseEntity<CommonResponse<Void>> response = handler.handleBusinessException(
			new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND)
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isEqualTo(CommonResponse.error(ErrorCode.ACCOUNT_NOT_FOUND));
	}

	@Test
	void unknownExceptionReturnsInternalServerErrorResponse() {
		ResponseEntity<CommonResponse<Void>> response = handler.handleException(
			new RuntimeException("unexpected")
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody()).isEqualTo(CommonResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
	}
}
