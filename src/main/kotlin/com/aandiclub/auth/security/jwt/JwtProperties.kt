package com.aandiclub.auth.security.jwt

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "security.jwt")
data class JwtProperties(
	val issuer: String,
	val audience: String,
	val secret: String,
	val accessTokenExpSeconds: Long,
	val refreshTokenExpSeconds: Long,
	val clockSkewSeconds: Long,
)
