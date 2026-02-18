package com.aandiclub.auth.security.service

import com.aandiclub.auth.common.error.AppException
import com.aandiclub.auth.common.error.ErrorCode
import com.aandiclub.auth.security.jwt.JwtPrincipal
import com.aandiclub.auth.security.jwt.JwtProperties
import com.aandiclub.auth.security.jwt.JwtToken
import com.aandiclub.auth.security.jwt.JwtTokenType
import com.aandiclub.auth.security.observability.NoopSecurityTelemetry
import com.aandiclub.auth.security.observability.SecurityTelemetry
import com.aandiclub.auth.user.domain.UserRole
import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.util.Date
import java.util.UUID

@Service
class JwtService(
	private val properties: JwtProperties,
	private val securityTelemetry: SecurityTelemetry = NoopSecurityTelemetry,
	private val clock: Clock = Clock.systemUTC(),
) {
	private val secret = properties.secret.toByteArray(StandardCharsets.UTF_8)
	private val signer: MACSigner = MACSigner(secret)
	private val verifier: MACVerifier = MACVerifier(secret)

	init {
		require(secret.size >= 32) { "JWT secret must be at least 32 bytes." }
	}

	fun issueAccessToken(userId: UUID, username: String, role: UserRole): JwtToken =
		issueToken(
			userId = userId,
			username = username,
			role = role,
			type = JwtTokenType.ACCESS,
			expiresInSeconds = properties.accessTokenExpSeconds,
		)

	fun issueRefreshToken(userId: UUID, username: String, role: UserRole): JwtToken =
		issueToken(
			userId = userId,
			username = username,
			role = role,
			type = JwtTokenType.REFRESH,
			expiresInSeconds = properties.refreshTokenExpSeconds,
		)

	fun verifyAndParse(token: String, expectedType: JwtTokenType): JwtPrincipal {
		val jwt = parse(token)
		if (!jwt.verify(verifier)) {
			unauthorized("Invalid token signature.")
		}

		val claims = jwt.jwtClaimsSet
		validateStandardClaims(claims)

		val tokenType = parseTokenType(claims.getStringClaim(CLAIM_TOKEN_TYPE))
		if (tokenType != expectedType) {
			unauthorized("Unexpected token type.")
		}

		val now = Instant.now(clock)
		val skew = properties.clockSkewSeconds
		val exp = claims.expirationTime?.toInstant()
			?: unauthorized("Missing token expiration.")
		if (exp.isBefore(now.minusSeconds(skew))) {
			unauthorized("Token is expired.")
		}

		val iat = claims.issueTime?.toInstant()
			?: unauthorized("Missing token issue time.")
		if (iat.isAfter(now.plusSeconds(skew))) {
			unauthorized("Token issue time is invalid.")
		}

		val userId = runCatching { UUID.fromString(claims.subject) }
			.getOrElse { unauthorized("Invalid token subject.") }
		val username = claims.getStringClaim(CLAIM_USERNAME)
			?: unauthorized("Missing username claim.")
		val role = runCatching { UserRole.valueOf(claims.getStringClaim(CLAIM_ROLE)) }
			.getOrElse { unauthorized("Invalid role claim.") }
		val jti = claims.getJWTID() ?: unauthorized("Missing token jti.")

		return JwtPrincipal(
			userId = userId,
			username = username,
			role = role,
			tokenType = tokenType,
			issuedAt = iat,
			expiresAt = exp,
			jti = jti,
		)
	}

	private fun issueToken(
		userId: UUID,
		username: String,
		role: UserRole,
		type: JwtTokenType,
		expiresInSeconds: Long,
	): JwtToken {
		val now = Instant.now(clock)
		val expiresAt = now.plusSeconds(expiresInSeconds)
		val claims = JWTClaimsSet.Builder()
			.issuer(properties.issuer)
			.audience(properties.audience)
			.subject(userId.toString())
			.issueTime(Date.from(now))
			.expirationTime(Date.from(expiresAt))
			.jwtID(UUID.randomUUID().toString())
			.claim(CLAIM_USERNAME, username)
			.claim(CLAIM_ROLE, role.name)
			.claim(CLAIM_TOKEN_TYPE, type.name)
			.build()

		val signedJWT = SignedJWT(JWSHeader(JWSAlgorithm.HS256), claims)
		try {
			signedJWT.sign(signer)
		} catch (ex: JOSEException) {
			throw AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to sign token.")
		}

		return JwtToken(
			value = signedJWT.serialize(),
			expiresAt = expiresAt,
			tokenType = type,
		)
	}

	private fun parse(token: String): SignedJWT = runCatching { SignedJWT.parse(token) }
		.getOrElse { unauthorized("Invalid token format.") }

	private fun validateStandardClaims(claims: JWTClaimsSet) {
		if (claims.issuer != properties.issuer) {
			unauthorized("Invalid token issuer.")
		}
		if (!claims.audience.contains(properties.audience)) {
			unauthorized("Invalid token audience.")
		}
	}

	private fun parseTokenType(raw: String?): JwtTokenType = runCatching {
		JwtTokenType.valueOf(raw ?: "")
	}.getOrElse {
		unauthorized("Invalid token type claim.")
	}

	private fun unauthorized(message: String): Nothing {
		securityTelemetry.tokenValidationFailed(message)
		throw AppException(ErrorCode.UNAUTHORIZED, message)
	}

	companion object {
		private const val CLAIM_USERNAME = "username"
		private const val CLAIM_ROLE = "role"
		private const val CLAIM_TOKEN_TYPE = "token_type"
	}
}
