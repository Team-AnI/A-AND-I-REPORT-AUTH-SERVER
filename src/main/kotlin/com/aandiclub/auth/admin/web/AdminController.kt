package com.aandiclub.auth.admin.web

import com.aandiclub.auth.admin.service.AdminService
import com.aandiclub.auth.admin.web.dto.AdminUserSummary
import com.aandiclub.auth.admin.web.dto.CreateAdminUserRequest
import com.aandiclub.auth.admin.web.dto.CreateAdminUserResponse
import com.aandiclub.auth.admin.web.dto.ResetPasswordResponse
import com.aandiclub.auth.common.api.ApiResponse
import com.aandiclub.auth.security.auth.AuthenticatedUser
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
@RequestMapping("/v1/admin")
class AdminController(
	private val adminService: AdminService,
) {
	@GetMapping("/ping")
	fun ping(): ApiResponse<Map<String, Boolean>> = ApiResponse.success(mapOf("ok" to true))

	@GetMapping("/users")
	fun getUsers(): Mono<ApiResponse<List<AdminUserSummary>>> =
		adminService.getUsers().map { ApiResponse.success(it) }

	@PostMapping("/users")
	fun createUser(@RequestBody request: CreateAdminUserRequest): Mono<ApiResponse<CreateAdminUserResponse>> =
		adminService.createUser(request).map { ApiResponse.success(it) }

	@PostMapping("/users/{id}/reset-password")
	fun resetPassword(@PathVariable id: UUID): Mono<ApiResponse<ResetPasswordResponse>> =
		adminService.resetPassword(id).map { ApiResponse.success(it) }

	@DeleteMapping("/users/{id}")
	fun deleteUser(
		@PathVariable id: UUID,
		@AuthenticationPrincipal actor: AuthenticatedUser,
	): Mono<ResponseEntity<Void>> =
		adminService.deleteUser(targetUserId = id, actorUserId = actor.userId)
			.thenReturn(ResponseEntity.noContent().build())
}
