package com.aandiclub.auth.user.service.impl

import com.aandiclub.auth.security.auth.AuthenticatedUser
import com.aandiclub.auth.user.service.UserService
import com.aandiclub.auth.user.web.dto.MeResponse
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class UserServiceImpl : UserService {
	override fun getMe(user: AuthenticatedUser): Mono<MeResponse> =
		Mono.just(MeResponse(id = user.userId, username = user.username, role = user.role))
}
