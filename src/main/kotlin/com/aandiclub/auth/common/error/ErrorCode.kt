package com.aandiclub.auth.common.error

import org.springframework.http.HttpStatus

enum class ErrorCode(
	val status: HttpStatus,
	val defaultMessage: String,
) {
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request."),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Unauthorized."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "Forbidden."),
	NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error."),
}
