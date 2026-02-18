package com.aandiclub.auth.admin.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.bootstrap-admin")
data class BootstrapAdminProperties(
	val enabled: Boolean = true,
	val username: String = "admin",
	val password: String = "",
)
