package com.aandiclub.auth.common

import com.aandiclub.auth.security.config.AppCorsProperties
import io.kotest.core.extensions.Extension
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest
class CorsPolicyTest : StringSpec() {

	@Autowired
	private lateinit var applicationContext: ApplicationContext

	@Autowired
	private lateinit var corsProperties: AppCorsProperties

	override fun extensions(): List<Extension> = listOf(SpringExtension)

	init {
		"CORS allowed origins should include localhost for local swagger testing" {
			check(corsProperties.allowedOriginsList().contains("http://localhost:8080"))
		}

		"CORS preflight from allowed origin should pass for login endpoint" {
			webClient().options()
				.uri("/v1/auth/login")
				.header("Origin", "http://localhost:8080")
				.header("Access-Control-Request-Method", "POST")
				.header("Access-Control-Request-Headers", "content-type")
				.exchange()
				.expectStatus().is2xxSuccessful
				.expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:8080")
		}
	}

	private fun webClient(): WebTestClient = WebTestClient.bindToApplicationContext(applicationContext).build()
}
