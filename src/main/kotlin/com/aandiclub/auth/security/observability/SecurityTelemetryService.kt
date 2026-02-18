package com.aandiclub.auth.security.observability

import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SecurityTelemetryService(
	private val meterRegistry: MeterRegistry,
) : SecurityTelemetry {
	override fun loginFailed(username: String) {
		meterRegistry.counter("auth.login.failures").increment()
		logger.warn("security_audit event=login_failed username={}", maskUsername(username))
	}

	override fun tokenValidationFailed(reason: String) {
		meterRegistry.counter("auth.jwt.validation.failures").increment()
		logger.warn("security_audit event=token_validation_failed reason={}", reason)
	}

	override fun refreshBlocked(refreshTokenKey: String) {
		meterRegistry.counter("auth.refresh.blocked").increment()
		logger.warn("security_audit event=refresh_blocked token_key={}", maskTokenKey(refreshTokenKey))
	}

	private fun maskUsername(username: String): String =
		if (username.length <= 2) "**" else username.take(2) + "***"

	private fun maskTokenKey(key: String): String =
		if (key.length <= 18) "masked" else key.take(18) + "***"

	companion object {
		private val logger = LoggerFactory.getLogger(SecurityTelemetryService::class.java)
	}
}
