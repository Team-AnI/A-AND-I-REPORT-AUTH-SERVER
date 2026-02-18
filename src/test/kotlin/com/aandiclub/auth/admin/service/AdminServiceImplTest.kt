package com.aandiclub.auth.admin.service

import com.aandiclub.auth.admin.password.CredentialGenerator
import com.aandiclub.auth.admin.sequence.UsernameSequenceService
import com.aandiclub.auth.admin.service.impl.AdminServiceImpl
import com.aandiclub.auth.admin.web.dto.CreateAdminUserRequest
import com.aandiclub.auth.security.service.PasswordService
import com.aandiclub.auth.user.domain.UserEntity
import com.aandiclub.auth.user.domain.UserRole
import com.aandiclub.auth.user.repository.UserRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.util.UUID

class AdminServiceImplTest : FunSpec({
	val userRepository = mockk<UserRepository>()
	val usernameSequenceService = mockk<UsernameSequenceService>()
	val credentialGenerator = mockk<CredentialGenerator>()
	val passwordService = mockk<PasswordService>()

	val service = AdminServiceImpl(
		userRepository = userRepository,
		usernameSequenceService = usernameSequenceService,
		credentialGenerator = credentialGenerator,
		passwordService = passwordService,
	)

	test("createUser should generate user_XX and return one-time password") {
		val savedUser = UserEntity(
			id = UUID.randomUUID(),
			username = "user_01",
			passwordHash = "hashed-password",
			role = UserRole.USER,
		)
		val savedEntitySlot = slot<UserEntity>()

		every { usernameSequenceService.nextSequence() } returns Mono.just(1)
		every { credentialGenerator.randomPassword(32) } returns "A".repeat(32)
		every { passwordService.hash("A".repeat(32)) } returns "hashed-password"
		every { userRepository.save(capture(savedEntitySlot)) } returns Mono.just(savedUser)

		StepVerifier.create(service.createUser(CreateAdminUserRequest(role = UserRole.USER)))
			.assertNext { response ->
				response.username shouldBe "user_01"
				response.password shouldBe "A".repeat(32)
				response.role shouldBe UserRole.USER
			}
			.verifyComplete()

		savedEntitySlot.captured.username shouldBe "user_01"
		savedEntitySlot.captured.passwordHash shouldBe "hashed-password"
	}

	test("getUsers should return summarized users") {
		every { userRepository.findAll() } returns Flux.just(
			UserEntity(
				id = UUID.randomUUID(),
				username = "user_01",
				passwordHash = "h1",
				role = UserRole.USER,
			),
			UserEntity(
				id = UUID.randomUUID(),
				username = "admin",
				passwordHash = "h2",
				role = UserRole.ADMIN,
			),
		)

		StepVerifier.create(service.getUsers())
			.assertNext { users ->
				users.size shouldBe 2
				users[0].username shouldBe "user_01"
				users[1].role shouldBe UserRole.ADMIN
			}
			.verifyComplete()
	}
})
