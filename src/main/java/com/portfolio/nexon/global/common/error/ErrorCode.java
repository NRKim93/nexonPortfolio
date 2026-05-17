package com.portfolio.nexon.global.common.error;

public enum ErrorCode {

	BAD_REQUEST("ERR-001", 400, "BAD REQUEST"),
	INTERNAL_SERVER_ERROR("ERR-003", 500, "INTERNAL SERVER ERROR"),
	UNKNOWN_ERROR("ERR-004", 999, "UNKNOWN ERROR"),
	AUTHENTICATION_FAILED("AUTH-001", 401, "AUTHENTICATION FAILED"),
	FORBIDDEN("AUTH-002", 403, "FORBIDDEN"),
	ACCOUNT_NOT_FOUND("ACCOUNT-001", 404, "ACCOUNT NOT FOUND"),
	OAUTH_ACCOUNT_ALREADY_LINKED("ACCOUNT-002", 400, "OAUTH ACCOUNT ALREADY LINKED"),
	NO_ITEM("BILLING-001", 404, "NO ITEM"),
	PRODUCT_NOT_AVAILABLE("BILLING-002", 400, "PRODUCT NOT AVAILABLE"),
	NO_PURCHASE_HISTORY("REFUND-001", 404, "NO PURCHASE HISTORY"),
	ALREADY_REFUNDED("REFUND-002", 400, "ALREADY REFUNDED"),
	REFUND_PERIOD_EXPIRED("REFUND-003", 400, "REFUND PERIOD EXPIRED"),
	USED_ITEM("REFUND-004", 400, "USED ITEM"),
	NO_TARGET_USER("ADMIN-001", 404, "NO TARGET USER"),
	CANNOT_CHANGE_ACCOUNT_STATUS("ADMIN-002", 400, "CAN'T CHANGE ACCOUNT STATUS");

	private final String code;
	private final int httpStatus;
	private final String message;

	ErrorCode(String code, int httpStatus, String message) {
		this.code = code;
		this.httpStatus = httpStatus;
		this.message = message;
	}

	public String code() {
		return code;
	}

	public int httpStatus() {
		return httpStatus;
	}

	public String message() {
		return message;
	}
}
