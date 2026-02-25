package com.aandiclub.auth.user.web

import com.aandiclub.auth.common.api.ApiResponse
import com.aandiclub.auth.common.error.AppException
import com.aandiclub.auth.common.error.ErrorCode
import com.aandiclub.auth.security.auth.AuthenticatedUser
import com.aandiclub.auth.user.service.UserService
import com.aandiclub.auth.user.web.dto.ChangePasswordRequest
import com.aandiclub.auth.user.web.dto.ChangePasswordResponse
import com.aandiclub.auth.user.web.dto.CreateProfileImageUploadUrlRequest
import com.aandiclub.auth.user.web.dto.CreateProfileImageUploadUrlResponse
import com.aandiclub.auth.user.web.dto.MeResponse
import com.aandiclub.auth.user.web.dto.UpdateProfileRequest
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.FormFieldPart
import org.springframework.http.codec.multipart.Part
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
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

	@PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
	fun updateProfile(
		@AuthenticationPrincipal user: AuthenticatedUser,
		@RequestPart("nickname", required = false) nickname: Part?,
		@RequestPart("profileImage", required = false) profileImage: FilePart?,
	): Mono<ApiResponse<MeResponse>> {
		val nicknameValue = when (nickname) {
			null -> null
			is FormFieldPart -> nickname.value()
			else -> return Mono.error(AppException(ErrorCode.INVALID_REQUEST, "nickname must be a text form field."))
		}
		return userService.updateProfile(user, nicknameValue, profileImage, null)
			.map { ApiResponse.success(it) }
	}

	@PatchMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
	fun updateProfileAsJson(
		@AuthenticationPrincipal user: AuthenticatedUser,
		@Valid @RequestBody request: UpdateProfileRequest,
	): Mono<ApiResponse<MeResponse>> =
		userService.updateProfile(user, request.nickname, null, request.profileImageUrl)
			.map { ApiResponse.success(it) }

	@PostMapping("/profile-image/upload-url")
	fun createProfileImageUploadUrl(
		@AuthenticationPrincipal user: AuthenticatedUser,
		@Valid @RequestBody request: CreateProfileImageUploadUrlRequest,
	): Mono<ApiResponse<CreateProfileImageUploadUrlResponse>> =
		userService.createProfileImageUploadUrl(user, request).map { ApiResponse.success(it) }

	@PostMapping("/password")
	fun changePassword(
		@AuthenticationPrincipal user: AuthenticatedUser,
		@Valid @RequestBody request: ChangePasswordRequest,
	): Mono<ApiResponse<ChangePasswordResponse>> =
		userService.changePassword(user, request).map { ApiResponse.success(it) }

}
