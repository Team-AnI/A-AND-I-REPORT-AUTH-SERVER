package com.aandiclub.auth.common.web

import com.aandiclub.auth.common.api.ApiResponse
import com.aandiclub.auth.common.error.AppException
import com.aandiclub.auth.common.error.ErrorCode
import com.aandiclub.auth.common.service.PingService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/ping")
class PingController(
	private val pingService: PingService,
) {
	@GetMapping
	fun ping(): ApiResponse<Map<String, String>> =
		ApiResponse.success(mapOf("message" to pingService.ping()))

	@GetMapping("/error")
	fun error(): ApiResponse<Nothing> {
		throw AppException(ErrorCode.INVALID_REQUEST, "Forced validation error.")
	}
}
