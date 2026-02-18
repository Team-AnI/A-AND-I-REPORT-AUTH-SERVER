package com.aandiclub.auth.security.token

import com.aandiclub.auth.common.error.AppException
import com.aandiclub.auth.common.error.ErrorCode
import com.aandiclub.auth.security.observability.NoopSecurityTelemetry
import com.aandiclub.auth.security.observability.SecurityTelemetry
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.Duration
import java.time.Instant

@Service
class RefreshTokenStateService(
	private val redisTemplate: ReactiveStringRedisTemplate,
	private val tokenHashService: TokenHashService,
	private val securityTelemetry: SecurityTelemetry = NoopSecurityTelemetry,
	private val clock: Clock = Clock.systemUTC(),
) {
	fun markLoggedOut(refreshToken: String, refreshExpiresAt: Instant): Mono<Boolean> {
		val ttl = Duration.between(Instant.now(clock), refreshExpiresAt)
		if (ttl.isNegative || ttl.isZero) {
			return Mono.just(false)
		}

		return redisTemplate.opsForValue().set(redisKey(refreshToken), LOGOUT_MARKER, ttl)
	}

	fun isLoggedOut(refreshToken: String): Mono<Boolean> =
		redisTemplate.hasKey(redisKey(refreshToken)).defaultIfEmpty(false)

	fun rejectIfLoggedOut(refreshToken: String): Mono<Unit> =
		isLoggedOut(refreshToken).flatMap { loggedOut ->
			if (loggedOut) {
				securityTelemetry.refreshBlocked(redisKey(refreshToken))
				Mono.error(AppException(ErrorCode.UNAUTHORIZED, "Refresh token is logged out."))
			} else {
				Mono.just(Unit)
			}
		}

	private fun redisKey(refreshToken: String): String =
		"$KEY_PREFIX:${tokenHashService.sha256Hex(refreshToken)}"

	companion object {
		private const val KEY_PREFIX = "logout:refresh"
		private const val LOGOUT_MARKER = "1"
	}
}
