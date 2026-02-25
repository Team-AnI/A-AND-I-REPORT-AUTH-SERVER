package com.aandiclub.auth.user.web.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class UpdateProfileRequest(
	@field:Size(max = 40, message = "nickname length must be less than or equal to 40")
	@field:Pattern(
		regexp = "^[\\p{L}\\p{N} _.-]{1,40}$",
		message = "nickname allows only letters, numbers, spaces, underscores, hyphens, and dots.",
	)
	val nickname: String? = null,
	@field:Size(max = 2048, message = "profileImageUrl length must be less than or equal to 2048")
	val profileImageUrl: String? = null,
)

data class CreateProfileImageUploadUrlRequest(
	@field:NotBlank(message = "contentType is required")
	@field:Size(max = 100, message = "contentType length must be less than or equal to 100")
	val contentType: String,
	@field:Size(max = 255, message = "fileName length must be less than or equal to 255")
	val fileName: String? = null,
)

data class CreateProfileImageUploadUrlResponse(
	val uploadUrl: String,
	val profileImageUrl: String,
	val objectKey: String,
	val expiresInSeconds: Long,
)
