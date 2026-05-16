package com.portfolio.nexon.global.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.portfolio.nexon.global.common.error.ErrorCode;
import org.junit.jupiter.api.Test;

class CommonResponseTest {

	@Test
	void successResponseContainsSuccessCodeAndData() {
		CommonResponse<String> response = CommonResponse.success("data");

		assertThat(response.code()).isEqualTo("SUCCESS");
		assertThat(response.message()).isEqualTo("OK");
		assertThat(response.httpStatus()).isEqualTo(200);
		assertThat(response.data()).isEqualTo("data");
	}

	@Test
	void errorResponseContainsErrorCode() {
		CommonResponse<Void> response = CommonResponse.error(ErrorCode.BAD_REQUEST);

		assertThat(response.code()).isEqualTo("ERR-001");
		assertThat(response.message()).isEqualTo("BAD REQUEST");
		assertThat(response.httpStatus()).isEqualTo(400);
		assertThat(response.data()).isNull();
	}
}
