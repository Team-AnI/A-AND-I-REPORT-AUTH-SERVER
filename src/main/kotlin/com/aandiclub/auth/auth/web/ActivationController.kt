package com.aandiclub.auth.auth.web

import com.aandiclub.auth.auth.service.AuthService
import com.aandiclub.auth.auth.web.dto.ActivateRequest
import com.aandiclub.auth.auth.web.dto.ActivateResponse
import com.aandiclub.auth.common.api.ApiResponse
import jakarta.validation.Valid
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@Validated
class ActivationController(
	private val authService: AuthService,
) {
	@PostMapping("/activate")
	fun activate(@Valid @RequestBody request: ActivateRequest): Mono<ApiResponse<ActivateResponse>> =
		authService.activate(request).map { ApiResponse.success(it) }
}
