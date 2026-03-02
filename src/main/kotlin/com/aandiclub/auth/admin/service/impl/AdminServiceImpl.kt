package com.aandiclub.auth.admin.service.impl

import com.aandiclub.auth.admin.config.InviteProperties
import com.aandiclub.auth.admin.domain.UserInviteEntity
import com.aandiclub.auth.admin.invite.InviteTokenCacheService
import com.aandiclub.auth.admin.password.CredentialGenerator
import com.aandiclub.auth.admin.repository.UserInviteRepository
import com.aandiclub.auth.admin.sequence.UsernameSequenceService
import com.aandiclub.auth.admin.service.AdminService
import com.aandiclub.auth.admin.web.dto.AdminUserSummary
import com.aandiclub.auth.admin.web.dto.CreateAdminUserRequest
import com.aandiclub.auth.admin.web.dto.CreateAdminUserResponse
import com.aandiclub.auth.admin.web.dto.ProvisionType
import com.aandiclub.auth.admin.web.dto.ResetPasswordResponse
import com.aandiclub.auth.admin.web.dto.UpdateUserRoleResponse
import com.aandiclub.auth.common.error.AppException
import com.aandiclub.auth.common.error.ErrorCode
import com.aandiclub.auth.security.service.PasswordService
import com.aandiclub.auth.security.token.TokenHashService
import com.aandiclub.auth.user.domain.UserEntity
import com.aandiclub.auth.user.domain.UserRole
import com.aandiclub.auth.user.domain.UserTrack
import com.aandiclub.auth.user.repository.UserRepository
import com.aandiclub.auth.user.service.UserPublicCodeService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.Duration
import java.util.UUID

@Service
class AdminServiceImpl(
	private val userRepository: UserRepository,
	private val userInviteRepository: UserInviteRepository,
	private val inviteTokenCacheService: InviteTokenCacheService,
	private val usernameSequenceService: UsernameSequenceService,
	private val credentialGenerator: CredentialGenerator,
	private val passwordService: PasswordService,
	private val tokenHashService: TokenHashService,
	private val userPublicCodeService: UserPublicCodeService,
	private val inviteProperties: InviteProperties,
	private val clock: Clock = Clock.systemUTC(),
) : AdminService {
	override fun getUsers(): Mono<List<AdminUserSummary>> =
		userRepository.findAll()
			.flatMap { toAdminUserSummary(it, clock.instant()) }
			.collectList()

	override fun createUser(request: CreateAdminUserRequest): Mono<CreateAdminUserResponse> =
		Mono.zip(
			usernameSequenceService.nextSequence(),
			usernameSequenceService.nextCohortOrderSequence(request.cohort),
		).flatMap { tuple ->
			val username = "user_${tuple.t1.toString().padStart(2, '0')}"
			val cohortOrder = tuple.t2.toInt()
			val userTrack = userPublicCodeService.resolveTrack(request.role, request.userTrack)
			val publicCode = userPublicCodeService.generate(
				role = request.role,
				userTrack = userTrack,
				cohort = request.cohort,
				cohortOrder = cohortOrder,
			)
			when (request.provisionType) {
				ProvisionType.PASSWORD -> createPasswordProvisionedUser(username, request, userTrack, cohortOrder, publicCode)
				ProvisionType.INVITE -> createInviteProvisionedUser(username, request, userTrack, cohortOrder, publicCode)
			}
		}

	override fun resetPassword(userId: UUID): Mono<ResetPasswordResponse> =
		userRepository.findById(userId)
			.switchIfEmpty(Mono.error(AppException(ErrorCode.NOT_FOUND, "User not found.")))
			.flatMap { user ->
				val temporaryPassword = credentialGenerator.randomPassword(32)
				val hashedPassword = passwordService.hash(temporaryPassword)
				userRepository.save(
					user.copy(
						passwordHash = hashedPassword,
						forcePasswordChange = true,
					),
				).map {
					logger.warn("security_audit event=admin_password_reset user_id={} username={}", it.id, it.username)
					ResetPasswordResponse(temporaryPassword = temporaryPassword)
				}
			}

	override fun updateUserRole(
		targetUserId: UUID,
		role: UserRole,
		userTrack: UserTrack?,
		actorUserId: UUID,
	): Mono<UpdateUserRoleResponse> {
		if (targetUserId == actorUserId) {
			return Mono.error(AppException(ErrorCode.FORBIDDEN, "Admin cannot change own role."))
		}

		return userRepository.findById(targetUserId)
			.switchIfEmpty(Mono.error(AppException(ErrorCode.NOT_FOUND, "User not found.")))
			.flatMap { user ->
				val resolvedTrack = userPublicCodeService.resolveTrack(
					role = role,
					requestedTrack = if (role == UserRole.USER) userTrack ?: user.userTrack else null,
				)
				val recalculatedPublicCode = userPublicCodeService.generate(
					role = role,
					userTrack = resolvedTrack,
					cohort = user.cohort,
					cohortOrder = user.cohortOrder,
				)
				userRepository.save(
					user.copy(
						role = role,
						userTrack = resolvedTrack,
						publicCode = recalculatedPublicCode,
					),
				).map { saved ->
					logger.warn(
						"security_audit event=admin_user_role_changed user_id={} username={} old_role={} new_role={} old_track={} new_track={} public_code={}",
						saved.id,
						saved.username,
						user.role,
						saved.role,
						user.userTrack,
						saved.userTrack,
						saved.publicCode,
					)
					UpdateUserRoleResponse(
						id = requireNotNull(saved.id),
						username = saved.username,
						role = saved.role,
						userTrack = saved.userTrack,
						cohort = saved.cohort,
						cohortOrder = saved.cohortOrder,
						publicCode = saved.publicCode,
					)
				}
			}
	}

	override fun deleteUser(targetUserId: UUID, actorUserId: UUID): Mono<Void> {
		if (targetUserId == actorUserId) {
			return Mono.error(AppException(ErrorCode.FORBIDDEN, "Admin cannot delete own account."))
		}

		return userRepository.findById(targetUserId)
			.switchIfEmpty(Mono.error(AppException(ErrorCode.NOT_FOUND, "User not found.")))
			.flatMap { user ->
				userInviteRepository.findByUserIdOrderByCreatedAtDesc(requireNotNull(user.id))
					.concatMap { inviteTokenCacheService.deleteToken(it.tokenHash) }
					.then(userRepository.deleteById(requireNotNull(user.id)))
					.then(
						Mono.fromRunnable {
							logger.warn("security_audit event=admin_user_deleted user_id={} username={}", user.id, user.username)
						},
					)
					.then()
			}
	}

	private fun createPasswordProvisionedUser(
		username: String,
		request: CreateAdminUserRequest,
		userTrack: UserTrack,
		cohortOrder: Int,
		publicCode: String,
	): Mono<CreateAdminUserResponse> {
		val temporaryPassword = credentialGenerator.randomPassword(32)
		val hashedPassword = passwordService.hash(temporaryPassword)
		return userRepository.save(
			UserEntity(
				username = username,
				passwordHash = hashedPassword,
				role = request.role,
				userTrack = userTrack,
				cohort = request.cohort,
				cohortOrder = cohortOrder,
				publicCode = publicCode,
				forcePasswordChange = true,
				isActive = true,
			),
		).map { saved ->
			logger.warn(
				"security_audit event=admin_user_created type=password user_id={} username={} role={} track={} cohort={} cohort_order={} public_code={}",
				saved.id,
				saved.username,
				saved.role,
				saved.userTrack,
				saved.cohort,
				saved.cohortOrder,
				saved.publicCode,
			)
			CreateAdminUserResponse(
				id = requireNotNull(saved.id),
				username = saved.username,
				role = saved.role,
				userTrack = saved.userTrack,
				cohort = saved.cohort,
				cohortOrder = saved.cohortOrder,
				publicCode = saved.publicCode,
				provisionType = ProvisionType.PASSWORD,
				temporaryPassword = temporaryPassword,
			)
		}
	}

	private fun createInviteProvisionedUser(
		username: String,
		request: CreateAdminUserRequest,
		userTrack: UserTrack,
		cohortOrder: Int,
		publicCode: String,
	): Mono<CreateAdminUserResponse> {
		val rawInviteToken = credentialGenerator.randomToken()
		val hashedInviteToken = tokenHashService.sha256Hex(rawInviteToken)
		val placeholderPasswordHash = passwordService.hash(credentialGenerator.randomPassword(32))
		val now = clock.instant()
		val expiresAt = now.plus(Duration.ofHours(inviteProperties.expirationHours))
		return userRepository.save(
			UserEntity(
				username = username,
				passwordHash = placeholderPasswordHash,
				role = request.role,
				userTrack = userTrack,
				cohort = request.cohort,
				cohortOrder = cohortOrder,
				publicCode = publicCode,
				forcePasswordChange = true,
				isActive = false,
			),
		).flatMap { savedUser ->
			userInviteRepository.save(
				UserInviteEntity(
					userId = requireNotNull(savedUser.id),
					tokenHash = hashedInviteToken,
					expiresAt = expiresAt,
					createdAt = now,
				),
			).flatMap { savedInvite ->
				inviteTokenCacheService.cacheToken(savedInvite.tokenHash, rawInviteToken, expiresAt)
					.thenReturn(savedInvite)
			}.map {
				logger.warn(
					"security_audit event=admin_user_created type=invite user_id={} username={} role={} track={} cohort={} cohort_order={} public_code={} expires_at={}",
					savedUser.id,
					savedUser.username,
					savedUser.role,
					savedUser.userTrack,
					savedUser.cohort,
					savedUser.cohortOrder,
					savedUser.publicCode,
					expiresAt,
				)
				CreateAdminUserResponse(
					id = requireNotNull(savedUser.id),
					username = savedUser.username,
					role = savedUser.role,
					userTrack = savedUser.userTrack,
					cohort = savedUser.cohort,
					cohortOrder = savedUser.cohortOrder,
					publicCode = savedUser.publicCode,
					provisionType = ProvisionType.INVITE,
					inviteLink = "${inviteProperties.activationBaseUrl}?token=$rawInviteToken",
					expiresAt = expiresAt,
				)
			}
		}
	}

	private fun toAdminUserSummary(user: UserEntity, now: java.time.Instant): Mono<AdminUserSummary> {
		val baseSummary = AdminUserSummary(
			id = requireNotNull(user.id),
			username = user.username,
			role = user.role,
			userTrack = user.userTrack,
			cohort = user.cohort,
			cohortOrder = user.cohortOrder,
			publicCode = user.publicCode,
			isActive = user.isActive,
			forcePasswordChange = user.forcePasswordChange,
		)
		if (user.isActive) {
			return Mono.just(baseSummary)
		}

		return userInviteRepository
			.findByUserIdOrderByCreatedAtDesc(requireNotNull(user.id))
			.filter { it.usedAt == null && it.expiresAt.isAfter(now) }
			.next()
			.flatMap { invite ->
				inviteTokenCacheService.findToken(invite.tokenHash)
					.map { token ->
						baseSummary.copy(
							inviteLink = "${inviteProperties.activationBaseUrl}?token=$token",
							inviteExpiresAt = invite.expiresAt,
						)
					}
					.defaultIfEmpty(baseSummary.copy(inviteExpiresAt = invite.expiresAt))
			}
			.switchIfEmpty(Mono.just(baseSummary))
	}

	companion object {
		private val logger = LoggerFactory.getLogger(AdminServiceImpl::class.java)
	}
}
