package com.aandiclub.auth.user.web.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

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
