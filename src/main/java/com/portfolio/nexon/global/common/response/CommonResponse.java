package com.portfolio.nexon.global.common.response;

import com.portfolio.nexon.global.common.error.ErrorCode;

public record CommonResponse<T>(
	String code,
	String message,
	int httpStatus,
	T data
) {

	private static final String SUCCESS_CODE = "SUCCESS";
	private static final String SUCCESS_MESSAGE = "OK";
	private static final int SUCCESS_HTTP_STATUS = 200;

	public static <T> CommonResponse<T> success(T data) {
		return new CommonResponse<>(SUCCESS_CODE, SUCCESS_MESSAGE, SUCCESS_HTTP_STATUS, data);
	}

	public static CommonResponse<Void> success() {
		return new CommonResponse<>(SUCCESS_CODE, SUCCESS_MESSAGE, SUCCESS_HTTP_STATUS, null);
	}

	public static CommonResponse<Void> error(ErrorCode errorCode) {
		return of(errorCode, null);
	}

	public static <T> CommonResponse<T> of(ErrorCode errorCode, T data) {
		return new CommonResponse<>(
			errorCode.code(),
			errorCode.message(),
			errorCode.httpStatus(),
			data
		);
	}
}
