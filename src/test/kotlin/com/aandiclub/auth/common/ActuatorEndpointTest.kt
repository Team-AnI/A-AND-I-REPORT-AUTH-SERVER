package com.aandiclub.auth.common

import io.kotest.core.extensions.Extension
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest
class ActuatorEndpointTest : StringSpec() {

	@Autowired
	private lateinit var applicationContext: ApplicationContext

	override fun extensions(): List<Extension> = listOf(SpringExtension)

	init {
		"GET /actuator/health should be publicly accessible" {
			webClient().get()
				.uri("/actuator/health")
				.exchange()
				.expectStatus().value { status ->
					check(status == 200 || status == 503) {
						"Expected 200 or 503 for public health endpoint, but got $status"
					}
				}
		}
	}

	private fun webClient(): WebTestClient = WebTestClient.bindToApplicationContext(applicationContext).build()
}
