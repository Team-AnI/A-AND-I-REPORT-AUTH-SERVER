package com.aandiclub.auth.security.service

import com.aandiclub.auth.common.error.AppException
import com.aandiclub.auth.common.error.ErrorCode
import com.aandiclub.auth.security.jwt.JwtProperties
import com.aandiclub.auth.security.jwt.JwtTokenType
import com.aandiclub.auth.user.domain.UserRole
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64
import java.util.UUID

class JwtServiceTest : FunSpec({
	val now = Instant.parse("2026-02-18T00:00:00Z")
	val clock = Clock.fixed(now, ZoneOffset.UTC)
	val properties = JwtProperties(
		issuer = "aandiclub-auth",
		audience = "aandiclub-api",
		secret = "super-secret-key-at-least-32-bytes!!",
		accessTokenExpSeconds = 3600,
		refreshTokenExpSeconds = 7200,
		clockSkewSeconds = 30,
	)
	val jwtService = JwtService(
		properties = properties,
		clock = clock,
	)

	test("issue and verify access token") {
		val userId = UUID.randomUUID()
		val token = jwtService.issueAccessToken(userId, "user_01", UserRole.USER)

		val principal = jwtService.verifyAndParse(token.value, JwtTokenType.ACCESS)
		principal.userId shouldBe userId
		principal.username shouldBe "user_01"
		principal.role shouldBe UserRole.USER
		principal.tokenType shouldBe JwtTokenType.ACCESS
	}

	test("issue and verify refresh token") {
		val userId = UUID.randomUUID()
		val token = jwtService.issueRefreshToken(userId, "user_02", UserRole.ORGANIZER)

		val principal = jwtService.verifyAndParse(token.value, JwtTokenType.REFRESH)
		principal.userId shouldBe userId
		principal.role shouldBe UserRole.ORGANIZER
		principal.tokenType shouldBe JwtTokenType.REFRESH
	}

	test("expired token should be rejected") {
		val expiredService = JwtService(
			properties = properties.copy(accessTokenExpSeconds = -100),
			clock = clock,
		)
		val token = expiredService.issueAccessToken(UUID.randomUUID(), "expired", UserRole.USER)

		val ex = shouldThrow<AppException> {
			jwtService.verifyAndParse(token.value, JwtTokenType.ACCESS)
		}
		ex.errorCode shouldBe ErrorCode.UNAUTHORIZED
		ex.message shouldBe "Token is expired."
	}

	test("tampered signature should be rejected") {
		val token = jwtService.issueAccessToken(UUID.randomUUID(), "tamper", UserRole.USER)
		val chunks = token.value.split('.')
		val badPayload = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"x\"}".toByteArray())
		val tampered = "${chunks[0]}.$badPayload.${chunks[2]}"

		val ex = shouldThrow<AppException> {
			jwtService.verifyAndParse(tampered, JwtTokenType.ACCESS)
		}
		ex.errorCode shouldBe ErrorCode.UNAUTHORIZED
		ex.message shouldBe "Invalid token signature."
	}

	test("token type mismatch should be rejected") {
		val refreshToken = jwtService.issueRefreshToken(UUID.randomUUID(), "refresh_user", UserRole.USER)

		val ex = shouldThrow<AppException> {
			jwtService.verifyAndParse(refreshToken.value, JwtTokenType.ACCESS)
		}
		ex.errorCode shouldBe ErrorCode.UNAUTHORIZED
		ex.message shouldBe "Unexpected token type."
	}
})
