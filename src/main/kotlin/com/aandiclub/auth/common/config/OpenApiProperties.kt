package com.aandiclub.auth.common.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.openapi")
data class OpenApiProperties(
	val serverUrl: String = "",
)
