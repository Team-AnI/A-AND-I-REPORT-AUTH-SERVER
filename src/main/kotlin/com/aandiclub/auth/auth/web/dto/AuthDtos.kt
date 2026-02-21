package com.aandiclub.auth.auth.web.dto

import com.aandiclub.auth.user.domain.UserRole
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

data class LoginRequest(
	@field:NotBlank(message = "username is required")
	val username: String,
	@field:NotBlank(message = "password is required")
	val password: String,
)

data class RefreshRequest(
	@field:NotBlank(message = "refreshToken is required")
	val refreshToken: String,
)

data class LogoutRequest(
	@field:NotBlank(message = "refreshToken is required")
	val refreshToken: String,
)

data class LoginResponse(
	val accessToken: String,
	val refreshToken: String,
	val expiresIn: Long,
	val tokenType: String,
	val forcePasswordChange: Boolean,
	val user: LoginUser,
)

data class LoginUser(
	val id: UUID,
	val username: String,
	val role: UserRole,
)

data class RefreshResponse(
	val accessToken: String,
	val expiresIn: Long,
)

data class LogoutResponse(
	val success: Boolean,
)

data class ActivateRequest(
	@field:NotBlank(message = "token is required")
	val token: String,
	@field:NotBlank(message = "password is required")
	@field:Size(min = 12, max = 128, message = "password length must be between 12 and 128")
	val password: String,
)

data class ActivateResponse(
	val success: Boolean,
)
