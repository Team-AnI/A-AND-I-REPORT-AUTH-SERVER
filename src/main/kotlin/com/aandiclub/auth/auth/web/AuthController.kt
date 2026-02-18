package com.aandiclub.auth.auth.web

import com.aandiclub.auth.auth.service.AuthService
import com.aandiclub.auth.auth.web.dto.LoginRequest
import com.aandiclub.auth.auth.web.dto.LoginResponse
import com.aandiclub.auth.auth.web.dto.LogoutRequest
import com.aandiclub.auth.auth.web.dto.LogoutResponse
import com.aandiclub.auth.auth.web.dto.RefreshRequest
import com.aandiclub.auth.auth.web.dto.RefreshResponse
import com.aandiclub.auth.common.api.ApiResponse
import jakarta.validation.Valid
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/v1/auth")
@Validated
class AuthController(
	private val authService: AuthService,
) {
	@PostMapping("/login")
	fun login(@Valid @RequestBody request: LoginRequest): Mono<ApiResponse<LoginResponse>> =
		authService.login(request).map { ApiResponse.success(it) }

	@PostMapping("/refresh")
	fun refresh(@Valid @RequestBody request: RefreshRequest): Mono<ApiResponse<RefreshResponse>> =
		authService.refresh(request).map { ApiResponse.success(it) }

	@PostMapping("/logout")
	fun logout(@Valid @RequestBody request: LogoutRequest): Mono<ApiResponse<LogoutResponse>> =
		authService.logout(request).map { ApiResponse.success(it) }
}
