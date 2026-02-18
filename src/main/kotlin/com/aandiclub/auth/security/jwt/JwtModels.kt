package com.aandiclub.auth.security.jwt

import com.aandiclub.auth.user.domain.UserRole
import java.time.Instant
import java.util.UUID

data class JwtToken(
	val value: String,
	val expiresAt: Instant,
	val tokenType: JwtTokenType,
)

data class JwtPrincipal(
	val userId: UUID,
	val username: String,
	val role: UserRole,
	val tokenType: JwtTokenType,
	val issuedAt: Instant,
	val expiresAt: Instant,
	val jti: String,
)
