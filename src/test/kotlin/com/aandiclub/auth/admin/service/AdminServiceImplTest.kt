package com.aandiclub.auth.admin.service

import com.aandiclub.auth.admin.config.InviteProperties
import com.aandiclub.auth.admin.domain.UserInviteEntity
import com.aandiclub.auth.admin.invite.InviteTokenCacheService
import com.aandiclub.auth.admin.password.CredentialGenerator
import com.aandiclub.auth.admin.repository.UserInviteRepository
import com.aandiclub.auth.admin.sequence.UsernameSequenceService
import com.aandiclub.auth.admin.service.impl.AdminServiceImpl
import com.aandiclub.auth.admin.web.dto.CreateAdminUserRequest
import com.aandiclub.auth.admin.web.dto.ProvisionType
import com.aandiclub.auth.admin.web.dto.UpdateUserRequest
import com.aandiclub.auth.common.error.AppException
import com.aandiclub.auth.common.error.ErrorCode
import com.aandiclub.auth.security.service.PasswordService
import com.aandiclub.auth.security.token.TokenHashService
import com.aandiclub.auth.user.domain.UserEntity
import com.aandiclub.auth.user.domain.UserRole
import com.aandiclub.auth.user.domain.UserTrack
import com.aandiclub.auth.user.event.UserProfileEventPublisher
import com.aandiclub.auth.user.event.UserProfileUpdatedEvent
import com.aandiclub.auth.user.repository.UserRepository
import com.aandiclub.auth.user.service.UserPublicCodeService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class AdminServiceImplTest : FunSpec({
	val userRepository = mockk<UserRepository>()
	val userInviteRepository = mockk<UserInviteRepository>()
	val inviteTokenCacheService = mockk<InviteTokenCacheService>()
	val usernameSequenceService = mockk<UsernameSequenceService>()
	val credentialGenerator = mockk<CredentialGenerator>()
	val passwordService = mockk<PasswordService>()
	val tokenHashService = mockk<TokenHashService>()
	val userProfileEventPublisher = mockk<UserProfileEventPublisher>()
	val clock = Clock.fixed(Instant.parse("2026-02-18T00:00:00Z"), ZoneOffset.UTC)

	val service = AdminServiceImpl(
		userRepository = userRepository,
		userInviteRepository = userInviteRepository,
		inviteTokenCacheService = inviteTokenCacheService,
		usernameSequenceService = usernameSequenceService,
		credentialGenerator = credentialGenerator,
		passwordService = passwordService,
		tokenHashService = tokenHashService,
		userPublicCodeService = UserPublicCodeService(),
		userProfileEventPublisher = userProfileEventPublisher,
		inviteProperties = InviteProperties(
			activationBaseUrl = "https://your-domain.com/activate",
			expirationHours = 72,
		),
		clock = clock,
	)
	every { userProfileEventPublisher.publishUserProfileUpdated(any()) } returns Mono.empty()

	test("createUser PASSWORD should generate temporary password and public code") {
		val savedUser = UserEntity(
			id = UUID.randomUUID(),
			username = "user_01",
			passwordHash = "hashed-password",
			role = UserRole.USER,
			userTrack = UserTrack.NO,
			cohort = 4,
			cohortOrder = 1,
			publicCode = "#NO401",
			forcePasswordChange = true,
		)
		val savedEntitySlot = slot<UserEntity>()

		every { usernameSequenceService.nextSequence() } returns Mono.just(1)
		every { usernameSequenceService.nextCohortOrderSequence(4) } returns Mono.just(1)
		every { credentialGenerator.randomPassword(32) } returns "A".repeat(32)
		every { passwordService.hash("A".repeat(32)) } returns "hashed-password"
		every { userRepository.save(capture(savedEntitySlot)) } returns Mono.just(savedUser)

		StepVerifier.create(
			service.createUser(
				CreateAdminUserRequest(cohort = 4, role = UserRole.USER, provisionType = ProvisionType.PASSWORD),
			),
		)
			.assertNext { response ->
				response.username shouldBe "user_01"
				response.temporaryPassword shouldBe "A".repeat(32)
				response.role shouldBe UserRole.USER
				response.userTrack shouldBe UserTrack.NO
				response.cohort shouldBe 4
				response.cohortOrder shouldBe 1
				response.publicCode shouldBe "#NO401"
				response.provisionType shouldBe ProvisionType.PASSWORD
			}
			.verifyComplete()

		savedEntitySlot.captured.username shouldBe "user_01"
		savedEntitySlot.captured.passwordHash shouldBe "hashed-password"
		savedEntitySlot.captured.forcePasswordChange shouldBe true
		savedEntitySlot.captured.userTrack shouldBe UserTrack.NO
		savedEntitySlot.captured.publicCode shouldBe "#NO401"
	}

	test("createUser INVITE should return one-time invite link and inactive account") {
		val userId = UUID.randomUUID()
		val savedUser = UserEntity(
			id = userId,
			username = "user_02",
			passwordHash = "placeholder-hash",
			role = UserRole.USER,
			userTrack = UserTrack.NO,
			cohort = 4,
			cohortOrder = 2,
			publicCode = "#NO402",
			forcePasswordChange = true,
			isActive = false,
		)
		val savedUserSlot = slot<UserEntity>()
		val inviteSlot = slot<UserInviteEntity>()

		every { usernameSequenceService.nextSequence() } returns Mono.just(2)
		every { usernameSequenceService.nextCohortOrderSequence(4) } returns Mono.just(2)
		every { credentialGenerator.randomToken(any()) } returns "invite-token"
		every { tokenHashService.sha256Hex("invite-token") } returns "invite-hash"
		every { credentialGenerator.randomPassword(32) } returns "B".repeat(32)
		every { passwordService.hash("B".repeat(32)) } returns "placeholder-hash"
		every { userRepository.save(capture(savedUserSlot)) } returns Mono.just(savedUser)
		every { userInviteRepository.save(capture(inviteSlot)) } answers { Mono.just(firstArg()) }
		every { inviteTokenCacheService.cacheToken("invite-hash", "invite-token", any()) } returns Mono.just(true)

		StepVerifier.create(
			service.createUser(
				CreateAdminUserRequest(cohort = 4),
			),
		).assertNext { response ->
			response.username shouldBe "user_02"
			response.provisionType shouldBe ProvisionType.INVITE
			response.inviteLink shouldBe "https://your-domain.com/activate?token=invite-token"
			response.temporaryPassword shouldBe null
			response.publicCode shouldBe "#NO402"
		}.verifyComplete()

		savedUserSlot.captured.isActive shouldBe false
		savedUserSlot.captured.forcePasswordChange shouldBe true
		inviteSlot.captured.userId shouldBe userId
		inviteSlot.captured.tokenHash shouldBe "invite-hash"
	}

	test("resetPassword should set forcePasswordChange and return temporary password") {
		val userId = UUID.randomUUID()
		val user = UserEntity(
			id = userId,
			username = "user_03",
			passwordHash = "old",
			role = UserRole.USER,
		)
		val savedSlot = slot<UserEntity>()

		every { userRepository.findById(userId) } returns Mono.just(user)
		every { credentialGenerator.randomPassword(32) } returns "C".repeat(32)
		every { passwordService.hash("C".repeat(32)) } returns "new-hash"
		every { userRepository.save(capture(savedSlot)) } returns Mono.just(user.copy(passwordHash = "new-hash", forcePasswordChange = true))

		StepVerifier.create(service.resetPassword(userId))
			.assertNext { response ->
				response.temporaryPassword shouldBe "C".repeat(32)
			}
			.verifyComplete()

		savedSlot.captured.forcePasswordChange shouldBe true
	}

	test("getUsers should include inviteLink for inactive user with valid invite") {
		val userId = UUID.randomUUID()
		val now = Instant.parse("2026-02-18T00:00:00Z")
		val invite = UserInviteEntity(
			id = UUID.randomUUID(),
			userId = userId,
			tokenHash = "invite-hash",
			expiresAt = now.plusSeconds(3600),
			usedAt = null,
			createdAt = now,
		)
		every { userRepository.findAll() } returns Flux.just(
			UserEntity(
				id = userId,
				username = "user_10",
				passwordHash = "h1",
				role = UserRole.USER,
				cohort = 4,
				cohortOrder = 10,
				publicCode = "#NO410",
				nickname = "테스트닉",
				isActive = false,
				forcePasswordChange = true,
			),
		)
		every {
			userInviteRepository.findByUserIdOrderByCreatedAtDesc(userId)
		} returns Flux.just(invite)
		every { inviteTokenCacheService.findToken("invite-hash") } returns Mono.just("raw-token")

		StepVerifier.create(service.getUsers())
			.assertNext { users ->
				users.size shouldBe 1
				users[0].inviteLink shouldBe "https://your-domain.com/activate?token=raw-token"
				users[0].publicCode shouldBe "#NO410"
				users[0].nickname shouldBe "테스트닉"
			}
			.verifyComplete()
	}

	test("updateUserRole should update target user's role and public code") {
		val actorId = UUID.randomUUID()
		val targetId = UUID.randomUUID()
		val originalUser = UserEntity(
			id = targetId,
			username = "member_01",
			passwordHash = "h1",
			role = UserRole.USER,
			userTrack = UserTrack.SP,
			cohort = 4,
			cohortOrder = 1,
			publicCode = "#SP401",
		)
		val savedSlot = slot<UserEntity>()
		val eventSlot = slot<UserProfileUpdatedEvent>()

		every { userRepository.findById(targetId) } returns Mono.just(originalUser)
		every { userRepository.save(capture(savedSlot)) } returns Mono.just(
			originalUser.copy(role = UserRole.ORGANIZER, userTrack = UserTrack.NO, publicCode = "#OR401"),
		)
		every { userProfileEventPublisher.publishUserProfileUpdated(capture(eventSlot)) } returns Mono.empty()

		StepVerifier.create(service.updateUserRole(targetId, UserRole.ORGANIZER, actorId))
			.assertNext { response ->
				response.id shouldBe targetId
				response.username shouldBe "member_01"
				response.role shouldBe UserRole.ORGANIZER
				response.publicCode shouldBe "#OR401"
			}
			.verifyComplete()

		savedSlot.captured.role shouldBe UserRole.ORGANIZER
		savedSlot.captured.userTrack shouldBe UserTrack.NO
		savedSlot.captured.publicCode shouldBe "#OR401"
		eventSlot.captured.userId shouldBe targetId.toString()
		eventSlot.captured.role shouldBe UserRole.ORGANIZER.name
		eventSlot.captured.publicCode shouldBe "#OR401"
	}

	test("updateUser should apply USER track and regenerate public code") {
		val actorId = UUID.randomUUID()
		val targetId = UUID.randomUUID()
		val originalUser = UserEntity(
			id = targetId,
			username = "member_02",
			passwordHash = "h1",
			role = UserRole.USER,
			userTrack = UserTrack.NO,
			cohort = 4,
			cohortOrder = 3,
			publicCode = "#NO403",
		)

		every { userRepository.findById(targetId) } returns Mono.just(originalUser)
		every { userRepository.save(any()) } returns Mono.just(
			originalUser.copy(userTrack = UserTrack.FL, publicCode = "#FL403"),
		)

		StepVerifier.create(
			service.updateUser(
				request = UpdateUserRequest(userId = targetId, userTrack = UserTrack.FL),
				actorUserId = actorId,
			),
		)
			.assertNext { response ->
				response.role shouldBe UserRole.USER
				response.userTrack shouldBe UserTrack.FL
				response.publicCode shouldBe "#FL403"
			}
			.verifyComplete()
	}

	test("updateUser should update role and recalculate track/public code") {
		val actorId = UUID.randomUUID()
		val targetId = UUID.randomUUID()
		val originalUser = UserEntity(
			id = targetId,
			username = "member_02b",
			passwordHash = "h1",
			role = UserRole.USER,
			userTrack = UserTrack.SP,
			cohort = 4,
			cohortOrder = 2,
			publicCode = "#SP402",
		)
		val eventSlot = slot<UserProfileUpdatedEvent>()

		every { userRepository.findById(targetId) } returns Mono.just(originalUser)
		every { userRepository.save(any()) } returns Mono.just(
			originalUser.copy(role = UserRole.ORGANIZER, userTrack = UserTrack.NO, publicCode = "#OR402"),
		)
		every { userProfileEventPublisher.publishUserProfileUpdated(capture(eventSlot)) } returns Mono.empty()

		StepVerifier.create(
			service.updateUser(
				request = UpdateUserRequest(userId = targetId, role = UserRole.ORGANIZER, userTrack = UserTrack.FL),
				actorUserId = actorId,
			),
		)
			.assertNext { response ->
				response.role shouldBe UserRole.ORGANIZER
				response.userTrack shouldBe UserTrack.NO
				response.publicCode shouldBe "#OR402"
			}
			.verifyComplete()

		eventSlot.captured.userId shouldBe targetId.toString()
		eventSlot.captured.role shouldBe UserRole.ORGANIZER.name
		eventSlot.captured.userTrack shouldBe UserTrack.NO.name
		eventSlot.captured.publicCode shouldBe "#OR402"
	}

	test("updateUser should update cohort and regenerate cohortOrder/publicCode") {
		val actorId = UUID.randomUUID()
		val targetId = UUID.randomUUID()
		val originalUser = UserEntity(
			id = targetId,
			username = "member_03",
			passwordHash = "h1",
			role = UserRole.USER,
			userTrack = UserTrack.NO,
			cohort = 4,
			cohortOrder = 3,
			publicCode = "#NO403",
		)
		val savedSlot = slot<UserEntity>()
		val eventSlot = slot<UserProfileUpdatedEvent>()

		every { userRepository.findById(targetId) } returns Mono.just(originalUser)
		every { usernameSequenceService.nextCohortOrderSequence(5) } returns Mono.just(1)
		every { userRepository.save(capture(savedSlot)) } answers { Mono.just(firstArg()) }
		every { userProfileEventPublisher.publishUserProfileUpdated(capture(eventSlot)) } returns Mono.empty()

		StepVerifier.create(
			service.updateUser(
				request = UpdateUserRequest(userId = targetId, userTrack = UserTrack.FL, cohort = 5),
				actorUserId = actorId,
			),
		)
			.assertNext { response ->
				response.role shouldBe UserRole.USER
				response.userTrack shouldBe UserTrack.FL
				response.cohort shouldBe 5
				response.cohortOrder shouldBe 1
				response.publicCode shouldBe "#FL501"
			}
			.verifyComplete()

		savedSlot.captured.cohort shouldBe 5
		savedSlot.captured.cohortOrder shouldBe 1
		savedSlot.captured.publicCode shouldBe "#FL501"
		eventSlot.captured.userId shouldBe targetId.toString()
		eventSlot.captured.userTrack shouldBe UserTrack.FL.name
		eventSlot.captured.cohort shouldBe 5
		eventSlot.captured.publicCode shouldBe "#FL501"
	}

	test("updateUserRole should reject self role change") {
		val adminId = UUID.randomUUID()
		StepVerifier.create(service.updateUserRole(adminId, UserRole.USER, adminId))
			.expectErrorSatisfies { ex ->
				(ex as AppException).errorCode shouldBe ErrorCode.FORBIDDEN
			}
			.verify()
	}

	test("updateUser should reject empty update payload") {
		val actorId = UUID.randomUUID()
		val targetId = UUID.randomUUID()

		StepVerifier.create(
			service.updateUser(
				request = UpdateUserRequest(userId = targetId),
				actorUserId = actorId,
			),
		)
			.expectErrorSatisfies { ex ->
				(ex as AppException).errorCode shouldBe ErrorCode.INVALID_REQUEST
			}
			.verify()
	}

	test("deleteUser should reject self deletion") {
		val adminId = UUID.randomUUID()
		StepVerifier.create(service.deleteUser(adminId, adminId))
			.expectErrorSatisfies { ex ->
				(ex as AppException).errorCode shouldBe ErrorCode.FORBIDDEN
			}
			.verify()
	}

	test("deleteUser should delete target user and cleanup invite tokens") {
		val actorId = UUID.randomUUID()
		val targetId = UUID.randomUUID()
		val targetUser = UserEntity(
			id = targetId,
			username = "user_delete",
			passwordHash = "h1",
			role = UserRole.USER,
		)
		val invite = UserInviteEntity(
			id = UUID.randomUUID(),
			userId = targetId,
			tokenHash = "invite-hash-delete",
			expiresAt = Instant.parse("2026-02-20T00:00:00Z"),
			createdAt = Instant.parse("2026-02-18T00:00:00Z"),
		)

		every { userRepository.findById(targetId) } returns Mono.just(targetUser)
		every { userInviteRepository.findByUserIdOrderByCreatedAtDesc(targetId) } returns Flux.just(invite)
		every { inviteTokenCacheService.deleteToken("invite-hash-delete") } returns Mono.just(true)
		every { userRepository.deleteById(targetId) } returns Mono.empty()

		StepVerifier.create(service.deleteUser(targetId, actorId))
			.verifyComplete()

		verify(exactly = 1) { userRepository.deleteById(targetId) }
	}
})
