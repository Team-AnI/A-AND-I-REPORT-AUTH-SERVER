package com.aandiclub.auth.security.config

import com.aandiclub.auth.admin.config.BootstrapAdminProperties
import com.aandiclub.auth.security.auth.JwtReactiveAuthenticationManager
import com.aandiclub.auth.security.filter.BearerTokenAuthenticationConverter
import com.aandiclub.auth.security.jwt.JwtProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint
import org.springframework.security.web.server.authorization.HttpStatusServerAccessDeniedHandler

@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(JwtProperties::class, BootstrapAdminProperties::class)
class SecurityConfig {

	@Bean
	fun securityWebFilterChain(
		http: ServerHttpSecurity,
		jwtReactiveAuthenticationManager: JwtReactiveAuthenticationManager,
	): SecurityWebFilterChain {
		val jwtAuthenticationWebFilter = AuthenticationWebFilter(jwtReactiveAuthenticationManager).apply {
			setServerAuthenticationConverter(BearerTokenAuthenticationConverter())
		}

		return http
			.csrf { it.disable() }
			.formLogin { it.disable() }
			.httpBasic { it.disable() }
			.exceptionHandling {
				it.authenticationEntryPoint(HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
				it.accessDeniedHandler(HttpStatusServerAccessDeniedHandler(HttpStatus.FORBIDDEN))
			}
			.authorizeExchange {
				it.pathMatchers(
					"/v1/auth/**",
					"/api/ping/**",
					"/v3/api-docs/**",
					"/swagger-ui.html",
					"/swagger-ui/**",
					"/actuator/health",
					"/actuator/info",
				).permitAll()
				it.pathMatchers("/v1/me").hasAnyRole("USER", "ORGANIZER", "ADMIN")
				it.pathMatchers("/v1/admin/**").hasRole("ADMIN")
				it.anyExchange().authenticated()
			}
			.addFilterAt(jwtAuthenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
			.build()
	}
}
