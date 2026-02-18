package com.aandiclub.auth.user.service

import com.aandiclub.auth.security.auth.AuthenticatedUser
import com.aandiclub.auth.user.web.dto.ChangePasswordRequest
import com.aandiclub.auth.user.web.dto.ChangePasswordResponse
import com.aandiclub.auth.user.web.dto.MeResponse
import reactor.core.publisher.Mono

interface UserService {
	fun getMe(user: AuthenticatedUser): Mono<MeResponse>
	fun changePassword(user: AuthenticatedUser, request: ChangePasswordRequest): Mono<ChangePasswordResponse>
}
