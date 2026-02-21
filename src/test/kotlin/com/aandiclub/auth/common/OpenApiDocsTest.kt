package com.aandiclub.auth.common

import io.kotest.core.extensions.Extension
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest
class OpenApiDocsTest : StringSpec() {

	@Autowired
	private lateinit var applicationContext: ApplicationContext

	override fun extensions(): List<Extension> = listOf(SpringExtension)

	init {
		"GET /v3/api-docs should be publicly accessible" {
			webClient().get()
				.uri("/v3/api-docs")
				.exchange()
				.expectStatus().isOk
				.expectBody()
				.jsonPath("$.openapi").exists()
				.jsonPath("$.paths['/v1/me'].post.requestBody.content['multipart/form-data']").exists()
		}

		"GET /swagger-ui.html should be publicly accessible" {
			webClient().get()
				.uri("/swagger-ui.html")
				.exchange()
				.expectStatus().is3xxRedirection
		}
	}

	private fun webClient(): WebTestClient = WebTestClient.bindToApplicationContext(applicationContext).build()
}
