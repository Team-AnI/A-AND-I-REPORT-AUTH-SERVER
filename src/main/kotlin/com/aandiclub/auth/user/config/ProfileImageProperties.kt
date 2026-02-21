package com.aandiclub.auth.user.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.profile-image")
data class ProfileImageProperties(
	val enabled: Boolean = false,
	val bucket: String = "",
	val region: String = "ap-northeast-2",
	val keyPrefix: String = "profiles",
	val publicBaseUrl: String = "",
	val uploadUrlExpirationSeconds: Long = 600,
	val maxUploadBytes: Long = 5_242_880,
	val allowedContentTypes: String = "image/jpeg,image/png,image/webp",
) {
	fun normalizedBucket(): String = bucket.trim()

	fun normalizedRegion(): String = region.trim()

	fun normalizedKeyPrefix(): String = keyPrefix.trim().trim('/').ifEmpty { "profiles" }

	fun normalizedPublicBaseUrl(): String = publicBaseUrl.trim().trimEnd('/')

	fun allowedContentTypesSet(): Set<String> =
		allowedContentTypes.split(",")
			.map { it.trim().lowercase() }
			.filter { it.isNotEmpty() }
			.toSet()
}
