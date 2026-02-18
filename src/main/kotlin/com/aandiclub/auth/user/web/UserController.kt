package com.aandiclub.auth.user.web

import com.aandiclub.auth.common.api.ApiResponse
import com.aandiclub.auth.security.auth.AuthenticatedUser
import com.aandiclub.auth.user.service.UserService
import com.aandiclub.auth.user.web.dto.ChangePasswordRequest
import com.aandiclub.auth.user.web.dto.ChangePasswordResponse
import com.aandiclub.auth.user.web.dto.MeResponse
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/v1/me")
class UserController(
	private val userService: UserService,
) {
	@GetMapping
	fun me(@AuthenticationPrincipal user: AuthenticatedUser): Mono<ApiResponse<MeResponse>> =
		userService.getMe(user).map { ApiResponse.success(it) }

	@PostMapping("/password")
	fun changePassword(
		@AuthenticationPrincipal user: AuthenticatedUser,
		@Valid @RequestBody request: ChangePasswordRequest,
	): Mono<ApiResponse<ChangePasswordResponse>> =
		userService.changePassword(user, request).map { ApiResponse.success(it) }
}
