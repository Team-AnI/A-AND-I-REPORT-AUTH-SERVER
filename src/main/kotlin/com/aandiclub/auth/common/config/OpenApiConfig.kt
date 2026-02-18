package com.aandiclub.auth.common.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(OpenApiProperties::class)
class OpenApiConfig {

	@Bean
	fun openApi(properties: OpenApiProperties): OpenAPI {
		val bearerSchemeName = "bearerAuth"
		return OpenAPI()
			.info(
				Info()
					.title("AANDI Club Auth API")
					.description("Core authentication and authorization service APIs")
					.version("v1"),
			)
			.components(
				Components().addSecuritySchemes(
					bearerSchemeName,
					SecurityScheme()
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT"),
				),
			)
			.addSecurityItem(SecurityRequirement().addList(bearerSchemeName))
			.apply {
				properties.serverUrl
					?.trim()
					?.takeIf { it.isNotBlank() }
					?.let { servers(listOf(Server().url(it))) }
			}
	}
}
