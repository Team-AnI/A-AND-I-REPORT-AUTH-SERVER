package com.aandiclub.auth.admin.web.dto

import com.aandiclub.auth.user.domain.UserRole
import java.util.UUID

data class AdminUserSummary(
	val id: UUID,
	val username: String,
	val role: UserRole,
)

data class CreateAdminUserRequest(
	val role: UserRole = UserRole.USER,
)

data class CreateAdminUserResponse(
	val id: UUID,
	val username: String,
	val password: String,
	val role: UserRole,
)
