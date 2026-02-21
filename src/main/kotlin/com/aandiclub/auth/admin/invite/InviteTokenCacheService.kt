package com.aandiclub.auth.admin.invite

import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.Duration
import java.time.Instant

@Service
class InviteTokenCacheService(
	private val redisTemplate: ReactiveStringRedisTemplate,
	private val clock: Clock = Clock.systemUTC(),
) {
	fun cacheToken(tokenHash: String, token: String, expiresAt: Instant): Mono<Boolean> {
		val ttl = Duration.between(Instant.now(clock), expiresAt)
		if (ttl.isNegative || ttl.isZero) {
			return Mono.just(false)
		}
		return redisTemplate.opsForValue().set(redisKey(tokenHash), token, ttl)
	}

	fun findToken(tokenHash: String): Mono<String> =
		redisTemplate.opsForValue().get(redisKey(tokenHash))

	fun deleteToken(tokenHash: String): Mono<Boolean> =
		redisTemplate.delete(redisKey(tokenHash)).map { it > 0 }.defaultIfEmpty(false)

	private fun redisKey(tokenHash: String): String =
		"$KEY_PREFIX:$tokenHash"

	companion object {
		private const val KEY_PREFIX = "invite:token"
	}
}
