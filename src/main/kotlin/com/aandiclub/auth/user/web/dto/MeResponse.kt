package com.aandiclub.auth.user.web.dto

import com.aandiclub.auth.user.domain.UserRole
import java.util.UUID

data class MeResponse(
	val id: UUID,
	val username: String,
	val role: UserRole,
)
