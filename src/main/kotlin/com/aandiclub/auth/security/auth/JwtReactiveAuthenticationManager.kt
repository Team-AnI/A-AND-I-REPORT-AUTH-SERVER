package com.aandiclub.auth.security.auth

import com.aandiclub.auth.security.jwt.JwtTokenType
import com.aandiclub.auth.security.service.JwtService
import com.aandiclub.auth.user.domain.UserRole
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class JwtReactiveAuthenticationManager(
	private val jwtService: JwtService,
) : ReactiveAuthenticationManager {
	override fun authenticate(authentication: Authentication): Mono<Authentication> {
		val token = authentication.credentials as? String ?: return Mono.empty()
		val principal = jwtService.verifyAndParse(token, JwtTokenType.ACCESS)
		val authenticatedUser = AuthenticatedUser(
			userId = principal.userId,
			username = principal.username,
			role = principal.role,
		)
		val authorities = inheritedRoles(principal.role)
			.map { SimpleGrantedAuthority("ROLE_${it.name}") }

		return Mono.just(UsernamePasswordAuthenticationToken(authenticatedUser, token, authorities))
	}

	private fun inheritedRoles(role: UserRole): List<UserRole> = when (role) {
		UserRole.ADMIN -> listOf(UserRole.ADMIN, UserRole.ORGANIZER, UserRole.USER)
		UserRole.ORGANIZER -> listOf(UserRole.ORGANIZER, UserRole.USER)
		UserRole.USER -> listOf(UserRole.USER)
	}
}
