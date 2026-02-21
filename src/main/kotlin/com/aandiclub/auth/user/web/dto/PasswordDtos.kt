package com.aandiclub.auth.user.web.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ChangePasswordRequest(
	@field:NotBlank(message = "currentPassword is required")
	val currentPassword: String,
	@field:NotBlank(message = "newPassword is required")
	@field:Size(min = 12, max = 128, message = "newPassword length must be between 12 and 128")
	val newPassword: String,
)

data class ChangePasswordResponse(
	val success: Boolean,
)
