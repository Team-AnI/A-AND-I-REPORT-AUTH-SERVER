package com.aandiclub.auth.user.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.profile")
data class ProfileProperties(
	val allowedImageHosts: String = "",
) {
	fun allowedImageHostsSet(): Set<String> =
		allowedImageHosts.split(",")
			.map { it.trim().lowercase() }
			.filter { it.isNotEmpty() }
			.toSet()
}
