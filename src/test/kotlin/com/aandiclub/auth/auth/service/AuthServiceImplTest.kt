package com.aandiclub.auth.auth.service

import com.aandiclub.auth.auth.service.impl.AuthServiceImpl
import com.aandiclub.auth.auth.web.dto.LoginRequest
import com.aandiclub.auth.auth.web.dto.LogoutRequest
import com.aandiclub.auth.auth.web.dto.RefreshRequest
import com.aandiclub.auth.common.error.AppException
import com.aandiclub.auth.common.error.ErrorCode
import com.aandiclub.auth.security.jwt.JwtPrincipal
import com.aandiclub.auth.security.jwt.JwtToken
import com.aandiclub.auth.security.jwt.JwtTokenType
import com.aandiclub.auth.security.service.JwtService
import com.aandiclub.auth.security.service.PasswordService
import com.aandiclub.auth.security.token.RefreshTokenStateService
import com.aandiclub.auth.user.domain.UserEntity
import com.aandiclub.auth.user.domain.UserRole
import com.aandiclub.auth.user.repository.UserRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class AuthServiceImplTest : FunSpec({
	val userRepository = mockk<UserRepository>()
	val passwordService = mockk<PasswordService>()
	val jwtService = mockk<JwtService>()
	val refreshTokenStateService = mockk<RefreshTokenStateService>()
	val clock = Clock.fixed(Instant.parse("2026-02-18T00:00:00Z"), ZoneOffset.UTC)

	val authService = AuthServiceImpl(
		userRepository = userRepository,
		passwordService = passwordService,
		jwtService = jwtService,
		refreshTokenStateService = refreshTokenStateService,
		clock = clock,
	)

	test("login should return access and refresh tokens") {
		val userId = UUID.randomUUID()
		val user = UserEntity(
			id = userId,
			username = "user_01",
			passwordHash = "hashed",
			role = UserRole.USER,
		)
		every { userRepository.findByUsername("user_01") } returns Mono.just(user)
		every { passwordService.matches("password", "hashed") } returns true
		every { jwtService.issueAccessToken(userId, "user_01", UserRole.USER) } returns JwtToken(
			value = "access-token",
			expiresAt = Instant.parse("2026-02-18T01:00:00Z"),
			tokenType = JwtTokenType.ACCESS,
		)
		every { jwtService.issueRefreshToken(userId, "user_01", UserRole.USER) } returns JwtToken(
			value = "refresh-token",
			expiresAt = Instant.parse("2026-04-19T00:00:00Z"),
			tokenType = JwtTokenType.REFRESH,
		)

		StepVerifier.create(authService.login(LoginRequest("user_01", "password")))
			.assertNext { response ->
				response.accessToken shouldBe "access-token"
				response.refreshToken shouldBe "refresh-token"
				response.tokenType shouldBe "Bearer"
				response.user.id shouldBe userId
			}
			.verifyComplete()
	}

	test("login should reject invalid password") {
		val user = UserEntity(
			id = UUID.randomUUID(),
			username = "user_01",
			passwordHash = "hashed",
			role = UserRole.USER,
		)
		every { userRepository.findByUsername("user_01") } returns Mono.just(user)
		every { passwordService.matches("wrong", "hashed") } returns false

		StepVerifier.create(authService.login(LoginRequest("user_01", "wrong")))
			.expectErrorSatisfies { ex ->
				(ex as AppException).errorCode shouldBe ErrorCode.UNAUTHORIZED
				ex.message shouldBe "Invalid username or password."
			}
			.verify()
	}

	test("refresh should reject logged-out token") {
		val refreshToken = "refresh-token"
		every { jwtService.verifyAndParse(refreshToken, JwtTokenType.REFRESH) } returns JwtPrincipal(
			userId = UUID.randomUUID(),
			username = "user_02",
			role = UserRole.USER,
			tokenType = JwtTokenType.REFRESH,
			issuedAt = Instant.parse("2026-02-18T00:00:00Z"),
			expiresAt = Instant.parse("2026-04-18T00:00:00Z"),
			jti = "jti-1",
		)
		every { refreshTokenStateService.rejectIfLoggedOut(refreshToken) } returns
			Mono.error(AppException(ErrorCode.UNAUTHORIZED, "Refresh token is logged out."))

		StepVerifier.create(authService.refresh(RefreshRequest(refreshToken)))
			.expectErrorSatisfies { ex ->
				(ex as AppException).errorCode shouldBe ErrorCode.UNAUTHORIZED
			}
			.verify()
	}

	test("logout should mark refresh token as logged-out") {
		val refreshToken = "refresh-token"
		val expiresAt = Instant.parse("2026-04-18T00:00:00Z")
		every { jwtService.verifyAndParse(refreshToken, JwtTokenType.REFRESH) } returns JwtPrincipal(
			userId = UUID.randomUUID(),
			username = "user_02",
			role = UserRole.USER,
			tokenType = JwtTokenType.REFRESH,
			issuedAt = Instant.parse("2026-02-18T00:00:00Z"),
			expiresAt = expiresAt,
			jti = "jti-2",
		)
		every { refreshTokenStateService.markLoggedOut(refreshToken, expiresAt) } returns Mono.just(true)

		StepVerifier.create(authService.logout(LogoutRequest(refreshToken)))
			.assertNext { response ->
				response.success shouldBe true
			}
			.verifyComplete()
	}

	test("refresh with invalid format should throw unauthorized") {
		every { jwtService.verifyAndParse("bad-token", JwtTokenType.REFRESH) } throws
			AppException(ErrorCode.UNAUTHORIZED, "Invalid token format.")

		StepVerifier.create(authService.refresh(RefreshRequest("bad-token")))
			.expectErrorSatisfies { ex ->
				(ex as AppException).errorCode shouldBe ErrorCode.UNAUTHORIZED
				ex.message shouldBe "Invalid token format."
			}
			.verify()
	}
})
