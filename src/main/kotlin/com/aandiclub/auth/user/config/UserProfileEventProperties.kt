package com.aandiclub.auth.user.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.user-profile-event")
data class UserProfileEventProperties(
	val enabled: Boolean = false,
	val topicArn: String = "",
	val region: String = "ap-northeast-2",
	val fifoMessageGroupId: String = "user-profile-updated",
) {
	fun normalizedTopicArn(): String = topicArn.trim()

	fun normalizedRegion(): String = region.trim()

	fun normalizedFifoMessageGroupId(): String = fifoMessageGroupId.trim().ifEmpty { "user-profile-updated" }

	fun isFifoTopic(): Boolean = normalizedTopicArn().endsWith(".fifo")
}
