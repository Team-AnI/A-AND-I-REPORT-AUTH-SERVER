package com.aandiclub.auth.security.token

import com.aandiclub.auth.common.error.AppException
import com.aandiclub.auth.common.error.ErrorCode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.data.redis.core.ReactiveValueOperations
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RefreshTokenStateServiceTest : FunSpec({
	val redisTemplate = mockk<ReactiveStringRedisTemplate>()
	val valueOperations = mockk<ReactiveValueOperations<String, String>>()
	val hashService = TokenHashService()
	val fixedNow = Instant.parse("2026-02-18T00:00:00Z")
	val clock = Clock.fixed(fixedNow, ZoneOffset.UTC)
	val service = RefreshTokenStateService(
		redisTemplate = redisTemplate,
		tokenHashService = hashService,
		clock = clock,
	)

	every { redisTemplate.opsForValue() } returns valueOperations

	test("markLoggedOut should store hashed key with remaining TTL") {
		val refreshToken = "refresh-token-value"
		val refreshExpiresAt = fixedNow.plusSeconds(300)
		val expectedKey = "logout:refresh:${hashService.sha256Hex(refreshToken)}"

		every { valueOperations.set(expectedKey, "1", any<java.time.Duration>()) } returns Mono.just(true)

		StepVerifier.create(service.markLoggedOut(refreshToken, refreshExpiresAt))
			.expectNext(true)
			.verifyComplete()
	}

	test("markLoggedOut should return false when token already expired") {
		StepVerifier.create(service.markLoggedOut("expired", fixedNow.minusSeconds(1)))
			.expectNext(false)
			.verifyComplete()
	}

	test("rejectIfLoggedOut should fail with unauthorized") {
		val refreshToken = "blocked-refresh-token"
		val expectedKey = "logout:refresh:${hashService.sha256Hex(refreshToken)}"
		every { redisTemplate.hasKey(expectedKey) } returns Mono.just(true)

		StepVerifier.create(service.rejectIfLoggedOut(refreshToken))
			.expectErrorSatisfies { ex ->
				(ex is AppException) shouldBe true
				val appException = ex as AppException
				appException.errorCode shouldBe ErrorCode.UNAUTHORIZED
				appException.message shouldBe "Refresh token is logged out."
			}
			.verify()
	}

	test("rejectIfLoggedOut should pass when not blocked") {
		val refreshToken = "active-refresh-token"
		val expectedKey = "logout:refresh:${hashService.sha256Hex(refreshToken)}"
		every { redisTemplate.hasKey(expectedKey) } returns Mono.just(false)

		StepVerifier.create(service.rejectIfLoggedOut(refreshToken))
			.expectNext(Unit)
			.verifyComplete()
	}

	test("token hash should be stable and not raw token") {
		val rawToken = "plain-refresh-token"
		val hash1 = hashService.sha256Hex(rawToken)
		val hash2 = hashService.sha256Hex(rawToken)

		hash1 shouldBe hash2
		(hash1 == rawToken) shouldBe false
	}
})
