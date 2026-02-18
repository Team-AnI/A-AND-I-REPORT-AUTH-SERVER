package com.aandiclub.auth.security.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer

@Configuration
class WebFluxCorsConfig(
	private val corsProperties: AppCorsProperties,
) : WebFluxConfigurer {
	override fun addCorsMappings(registry: CorsRegistry) {
		val allowedOrigins = corsProperties.allowedOriginsList()
		if (allowedOrigins.isEmpty()) {
			return
		}

		registry.addMapping("/**")
			.allowedOrigins(*allowedOrigins.toTypedArray())
			.allowedMethods(*corsProperties.allowedMethodsList().toTypedArray())
			.allowedHeaders(*corsProperties.allowedHeadersList().toTypedArray())
			.exposedHeaders(*corsProperties.exposedHeadersList().toTypedArray())
			.allowCredentials(corsProperties.allowCredentials)
			.maxAge(corsProperties.maxAgeSeconds)
	}
}
