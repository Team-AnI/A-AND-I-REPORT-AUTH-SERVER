package com.aandiclub.auth.security.observability

object NoopSecurityTelemetry : SecurityTelemetry {
	override fun loginFailed(username: String) = Unit
	override fun tokenValidationFailed(reason: String) = Unit
	override fun refreshBlocked(refreshTokenKey: String) = Unit
}
