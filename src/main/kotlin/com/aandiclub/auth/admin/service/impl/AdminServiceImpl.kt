package com.aandiclub.auth.admin.service.impl

import com.aandiclub.auth.admin.password.CredentialGenerator
import com.aandiclub.auth.admin.service.AdminService
import com.aandiclub.auth.admin.sequence.UsernameSequenceService
import com.aandiclub.auth.admin.web.dto.AdminUserSummary
import com.aandiclub.auth.admin.web.dto.CreateAdminUserRequest
import com.aandiclub.auth.admin.web.dto.CreateAdminUserResponse
import com.aandiclub.auth.security.service.PasswordService
import com.aandiclub.auth.user.domain.UserEntity
import com.aandiclub.auth.user.repository.UserRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class AdminServiceImpl(
	private val userRepository: UserRepository,
	private val usernameSequenceService: UsernameSequenceService,
	private val credentialGenerator: CredentialGenerator,
	private val passwordService: PasswordService,
) : AdminService {
	override fun getUsers(): Mono<List<AdminUserSummary>> =
		userRepository.findAll()
			.map { AdminUserSummary(id = requireNotNull(it.id), username = it.username, role = it.role) }
			.collectList()

	override fun createUser(request: CreateAdminUserRequest): Mono<CreateAdminUserResponse> =
		usernameSequenceService.nextSequence()
			.flatMap { sequence ->
				val username = "user_${sequence.toString().padStart(2, '0')}"
				val rawPassword = credentialGenerator.randomPassword(32)
				val hashedPassword = passwordService.hash(rawPassword)
				userRepository.save(
					UserEntity(
						username = username,
						passwordHash = hashedPassword,
						role = request.role,
					),
				).map { saved ->
					CreateAdminUserResponse(
						id = requireNotNull(saved.id),
						username = saved.username,
						password = rawPassword,
						role = saved.role,
					)
				}
			}
}
