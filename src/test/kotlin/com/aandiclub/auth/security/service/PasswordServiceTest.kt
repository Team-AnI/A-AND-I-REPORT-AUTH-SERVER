package com.aandiclub.auth.security.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PasswordServiceTest : FunSpec({
	val passwordService = PasswordService()

	test("hash should not equal raw and should verify") {
		val raw = "StrongPassword!123"
		val hashed = passwordService.hash(raw)

		(hashed == raw) shouldBe false
		passwordService.matches(raw, hashed) shouldBe true
		passwordService.matches("wrong-password", hashed) shouldBe false
	}
})
