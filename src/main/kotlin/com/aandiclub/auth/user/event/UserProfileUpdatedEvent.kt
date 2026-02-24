package com.aandiclub.auth.user.event

import java.time.Instant

data class UserProfileUpdatedEvent(
	val eventId: String,
	val type: String,
	val occurredAt: Instant,
	val userId: String,
	val nickname: String?,
	val profileImageUrl: String?,
	val version: Long,
)
