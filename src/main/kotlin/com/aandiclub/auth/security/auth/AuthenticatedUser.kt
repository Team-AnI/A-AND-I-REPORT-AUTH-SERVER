package com.aandiclub.auth.security.auth

import com.aandiclub.auth.user.domain.UserRole
import java.util.UUID

data class AuthenticatedUser(
	val userId: UUID,
	val username: String,
	val role: UserRole,
)
