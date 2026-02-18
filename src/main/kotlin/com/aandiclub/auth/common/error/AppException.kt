package com.aandiclub.auth.common.error

class AppException(
	val errorCode: ErrorCode,
	override val message: String = errorCode.defaultMessage,
) : RuntimeException(message)
