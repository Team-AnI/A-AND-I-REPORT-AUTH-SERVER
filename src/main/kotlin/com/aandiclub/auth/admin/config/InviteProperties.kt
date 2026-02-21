package com.aandiclub.auth.admin.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.invite")
data class InviteProperties(
	val activationBaseUrl: String = "https://your-domain.com/activate",
	val expirationHours: Long = 72,
)
