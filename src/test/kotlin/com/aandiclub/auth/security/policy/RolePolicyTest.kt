package com.aandiclub.auth.security.policy

import com.aandiclub.auth.user.domain.UserRole
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RolePolicyTest : FunSpec({
	val rolePolicy = RolePolicy()

	test("admin can access organizer and user resources") {
		rolePolicy.canAccess(UserRole.ORGANIZER, UserRole.ADMIN) shouldBe true
		rolePolicy.canAccess(UserRole.USER, UserRole.ADMIN) shouldBe true
	}

	test("organizer cannot access admin resource") {
		rolePolicy.canAccess(UserRole.ADMIN, UserRole.ORGANIZER) shouldBe false
	}

	test("user can only access user resource") {
		rolePolicy.canAccess(UserRole.USER, UserRole.USER) shouldBe true
		rolePolicy.canAccess(UserRole.ORGANIZER, UserRole.USER) shouldBe false
	}
})
