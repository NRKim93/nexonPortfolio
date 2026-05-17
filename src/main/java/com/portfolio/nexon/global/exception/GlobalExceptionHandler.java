package com.portfolio.nexon.global.exception;

import com.portfolio.nexon.global.common.error.ErrorCode;
import com.portfolio.nexon.global.common.response.CommonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<CommonResponse<Void>> handleBusinessException(BusinessException exception) {
		ErrorCode errorCode = exception.errorCode();

		return ResponseEntity
			.status(HttpStatusCode.valueOf(errorCode.httpStatus()))
			.body(CommonResponse.error(errorCode));
	}

	@ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
	public ResponseEntity<CommonResponse<Void>> handleValidationException(Exception exception) {
		log.debug("Validation exception occurred", exception);

		return ResponseEntity
			.badRequest()
			.body(CommonResponse.error(ErrorCode.BAD_REQUEST));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<CommonResponse<Void>> handleException(Exception exception) {
		log.error("Unhandled exception occurred", exception);

		return ResponseEntity
			.internalServerError()
			.body(CommonResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
	}
}
