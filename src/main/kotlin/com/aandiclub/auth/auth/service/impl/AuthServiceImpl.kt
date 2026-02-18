package com.aandiclub.auth.auth.service.impl

import com.aandiclub.auth.auth.service.AuthService
import com.aandiclub.auth.auth.web.dto.LoginRequest
import com.aandiclub.auth.auth.web.dto.LoginResponse
import com.aandiclub.auth.auth.web.dto.LoginUser
import com.aandiclub.auth.auth.web.dto.LogoutRequest
import com.aandiclub.auth.auth.web.dto.LogoutResponse
import com.aandiclub.auth.auth.web.dto.RefreshRequest
import com.aandiclub.auth.auth.web.dto.RefreshResponse
import com.aandiclub.auth.common.error.AppException
import com.aandiclub.auth.common.error.ErrorCode
import com.aandiclub.auth.security.jwt.JwtTokenType
import com.aandiclub.auth.security.observability.NoopSecurityTelemetry
import com.aandiclub.auth.security.observability.SecurityTelemetry
import com.aandiclub.auth.security.service.JwtService
import com.aandiclub.auth.security.service.PasswordService
import com.aandiclub.auth.security.token.RefreshTokenStateService
import com.aandiclub.auth.user.repository.UserRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.Duration

@Service
class AuthServiceImpl(
	private val userRepository: UserRepository,
	private val passwordService: PasswordService,
	private val jwtService: JwtService,
	private val refreshTokenStateService: RefreshTokenStateService,
	private val securityTelemetry: SecurityTelemetry = NoopSecurityTelemetry,
	private val clock: Clock = Clock.systemUTC(),
) : AuthService {
	override fun login(request: LoginRequest): Mono<LoginResponse> =
		userRepository.findByUsername(request.username)
			.switchIfEmpty(Mono.defer { invalidCredentials(request.username) })
			.flatMap { user ->
				if (!passwordService.matches(request.password, user.passwordHash)) {
					invalidCredentials(request.username)
				} else {
					val accessToken = jwtService.issueAccessToken(requireNotNull(user.id), user.username, user.role)
					val refreshToken = jwtService.issueRefreshToken(requireNotNull(user.id), user.username, user.role)
					Mono.just(
						LoginResponse(
							accessToken = accessToken.value,
							refreshToken = refreshToken.value,
							expiresIn = Duration.between(clock.instant(), accessToken.expiresAt).seconds,
							tokenType = "Bearer",
							user = LoginUser(
								id = requireNotNull(user.id),
								username = user.username,
								role = user.role,
							),
						),
					)
				}
			}

	override fun refresh(request: RefreshRequest): Mono<RefreshResponse> {
		return Mono.defer {
			val principal = jwtService.verifyAndParse(request.refreshToken, JwtTokenType.REFRESH)
			refreshTokenStateService.rejectIfLoggedOut(request.refreshToken)
				.then(
					Mono.fromSupplier {
						val accessToken = jwtService.issueAccessToken(principal.userId, principal.username, principal.role)
						RefreshResponse(
							accessToken = accessToken.value,
							expiresIn = Duration.between(clock.instant(), accessToken.expiresAt).seconds,
						)
					},
				)
		}
	}

	override fun logout(request: LogoutRequest): Mono<LogoutResponse> {
		return Mono.defer {
			val principal = jwtService.verifyAndParse(request.refreshToken, JwtTokenType.REFRESH)
			refreshTokenStateService.markLoggedOut(request.refreshToken, principal.expiresAt)
				.thenReturn(LogoutResponse(success = true))
		}
	}

	companion object {
		private const val INVALID_CREDENTIALS_MESSAGE = "Invalid username or password."
	}

	private fun invalidCredentials(username: String): Mono<Nothing> {
		securityTelemetry.loginFailed(username)
		return Mono.error(AppException(ErrorCode.UNAUTHORIZED, INVALID_CREDENTIALS_MESSAGE))
	}
}
