package com.aandiclub.auth.common.error

import com.aandiclub.auth.common.api.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ServerWebInputException

@RestControllerAdvice
class GlobalExceptionHandler {

	@ExceptionHandler(AppException::class)
	fun handleAppException(ex: AppException): ResponseEntity<ApiResponse<Nothing>> {
		val code = ex.errorCode
		return ResponseEntity
			.status(code.status)
			.body(ApiResponse.failure(code.name, ex.message))
	}

	@ExceptionHandler(WebExchangeBindException::class)
	fun handleValidationException(ex: WebExchangeBindException): ResponseEntity<ApiResponse<Nothing>> {
		val message = ex.bindingResult.fieldErrors.firstOrNull()?.defaultMessage
			?: ErrorCode.INVALID_REQUEST.defaultMessage
		return ResponseEntity
			.status(ErrorCode.INVALID_REQUEST.status)
			.body(ApiResponse.failure(ErrorCode.INVALID_REQUEST.name, message))
	}

	@ExceptionHandler(ServerWebInputException::class)
	fun handleInputException(ex: ServerWebInputException): ResponseEntity<ApiResponse<Nothing>> =
		ResponseEntity
			.status(ErrorCode.INVALID_REQUEST.status)
			.body(ApiResponse.failure(ErrorCode.INVALID_REQUEST.name, ErrorCode.INVALID_REQUEST.defaultMessage))

	@ExceptionHandler(Exception::class)
	fun handleUnhandledException(ex: Exception): ResponseEntity<ApiResponse<Nothing>> =
		ResponseEntity
			.status(ErrorCode.INTERNAL_SERVER_ERROR.status)
			.body(
				ApiResponse.failure(
					ErrorCode.INTERNAL_SERVER_ERROR.name,
					ErrorCode.INTERNAL_SERVER_ERROR.defaultMessage,
				),
			)
}
