package com.aandiclub.auth.security

import com.aandiclub.auth.security.service.JwtService
import com.aandiclub.auth.user.domain.UserRole
import io.kotest.core.extensions.Extension
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.UUID

@SpringBootTest
class AuthorizationMatrixTest : StringSpec() {

	@Autowired
	private lateinit var applicationContext: ApplicationContext

	@Autowired
	private lateinit var jwtService: JwtService

	override fun extensions(): List<Extension> = listOf(SpringExtension)

	init {
		"GET /v1/me requires authentication" {
			webClient().get()
				.uri("/v1/me")
				.exchange()
				.expectStatus().isUnauthorized
		}

		"GET /v1/me allows USER role" {
			val token = accessToken(UserRole.USER)
			webClient().get()
				.uri("/v1/me")
				.headers { it.setBearerAuth(token) }
				.exchange()
				.expectStatus().isOk
				.expectBody()
				.jsonPath("$.success").isEqualTo(true)
				.jsonPath("$.data.role").isEqualTo("USER")
		}

		"GET /v1/admin/ping denies USER role" {
			val token = accessToken(UserRole.USER)
			webClient().get()
				.uri("/v1/admin/ping")
				.headers { it.setBearerAuth(token) }
				.exchange()
				.expectStatus().isForbidden
		}

		"GET /v1/admin/ping denies ORGANIZER role" {
			val token = accessToken(UserRole.ORGANIZER)
			webClient().get()
				.uri("/v1/admin/ping")
				.headers { it.setBearerAuth(token) }
				.exchange()
				.expectStatus().isForbidden
		}

		"GET /v1/admin/ping allows ADMIN role" {
			val token = accessToken(UserRole.ADMIN)
			webClient().get()
				.uri("/v1/admin/ping")
				.headers { it.setBearerAuth(token) }
				.exchange()
				.expectStatus().isOk
				.expectBody()
				.jsonPath("$.success").isEqualTo(true)
				.jsonPath("$.data.ok").isEqualTo(true)
		}
	}

	private fun webClient(): WebTestClient = WebTestClient.bindToApplicationContext(applicationContext).build()

	private fun accessToken(role: UserRole): String =
		jwtService.issueAccessToken(UUID.randomUUID(), "tester_${role.name.lowercase()}", role).value
}
