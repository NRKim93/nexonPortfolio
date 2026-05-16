package com.portfolio.nexon.global.common.response;

import com.portfolio.nexon.global.common.error.ErrorCode;

public record CommonResponse<T>(
	String code,
	String message,
	int httpStatus,
	T data
) {

	public static <T> CommonResponse<T> success(T data) {
		return of(ErrorCode.SUCCESS, data);
	}

	public static CommonResponse<Void> success() {
		return of(ErrorCode.SUCCESS, null);
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
