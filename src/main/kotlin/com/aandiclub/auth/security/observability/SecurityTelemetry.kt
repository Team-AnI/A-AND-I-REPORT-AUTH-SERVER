package com.aandiclub.auth.security.observability

interface SecurityTelemetry {
	fun loginFailed(username: String)
	fun tokenValidationFailed(reason: String)
	fun refreshBlocked(refreshTokenKey: String)
}
