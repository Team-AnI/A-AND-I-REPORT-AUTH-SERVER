package com.aandiclub.auth.user.service.impl

import com.aandiclub.auth.security.auth.AuthenticatedUser
import com.aandiclub.auth.security.service.PasswordService
import com.aandiclub.auth.common.error.AppException
import com.aandiclub.auth.common.error.ErrorCode
import com.aandiclub.auth.user.repository.UserRepository
import com.aandiclub.auth.user.service.UserService
import com.aandiclub.auth.user.web.dto.ChangePasswordRequest
import com.aandiclub.auth.user.web.dto.ChangePasswordResponse
import com.aandiclub.auth.user.web.dto.MeResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class UserServiceImpl(
	private val userRepository: UserRepository,
	private val passwordService: PasswordService,
) : UserService {
	override fun getMe(user: AuthenticatedUser): Mono<MeResponse> =
		Mono.just(MeResponse(id = user.userId, username = user.username, role = user.role))

	override fun changePassword(user: AuthenticatedUser, request: ChangePasswordRequest): Mono<ChangePasswordResponse> =
		userRepository.findById(user.userId)
			.switchIfEmpty(Mono.error(AppException(ErrorCode.NOT_FOUND, "User not found.")))
			.flatMap { entity ->
				if (!passwordService.matches(request.currentPassword, entity.passwordHash)) {
					Mono.error(AppException(ErrorCode.UNAUTHORIZED, "Invalid username or password."))
				} else {
					userRepository.save(
						entity.copy(
							passwordHash = passwordService.hash(request.newPassword),
							forcePasswordChange = false,
						),
					).map {
						logger.warn("security_audit event=password_changed user_id={}", it.id)
						ChangePasswordResponse(success = true)
					}
				}
			}

	companion object {
		private val logger = LoggerFactory.getLogger(UserServiceImpl::class.java)
	}
}
