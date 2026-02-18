package com.aandiclub.auth.auth.service

import com.aandiclub.auth.auth.web.dto.LoginRequest
import com.aandiclub.auth.auth.web.dto.LoginResponse
import com.aandiclub.auth.auth.web.dto.LogoutRequest
import com.aandiclub.auth.auth.web.dto.LogoutResponse
import com.aandiclub.auth.auth.web.dto.RefreshRequest
import com.aandiclub.auth.auth.web.dto.RefreshResponse
import reactor.core.publisher.Mono

interface AuthService {
	fun login(request: LoginRequest): Mono<LoginResponse>
	fun refresh(request: RefreshRequest): Mono<RefreshResponse>
	fun logout(request: LogoutRequest): Mono<LogoutResponse>
}
