package com.aandiclub.auth.admin.web.dto

import com.aandiclub.auth.user.domain.UserRole
import java.time.Instant
import java.util.UUID

data class AdminUserSummary(
	val id: UUID,
	val username: String,
	val role: UserRole,
	val isActive: Boolean,
	val forcePasswordChange: Boolean,
	val inviteLink: String? = null,
	val inviteExpiresAt: Instant? = null,
)

data class CreateAdminUserRequest(
	val role: UserRole = UserRole.USER,
	val provisionType: ProvisionType = ProvisionType.INVITE,
)

data class CreateAdminUserResponse(
	val id: UUID,
	val username: String,
	val role: UserRole,
	val provisionType: ProvisionType,
	val inviteLink: String? = null,
	val expiresAt: Instant? = null,
	val temporaryPassword: String? = null,
)

data class ResetPasswordResponse(
	val temporaryPassword: String,
)

enum class ProvisionType {
	INVITE,
	PASSWORD,
}
