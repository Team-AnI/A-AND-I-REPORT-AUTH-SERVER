package com.aandiclub.auth.user.event

data class UserProfileUpdatedEvent(
	val eventId: String,
	val type: String,
	val occurredAt: String,
	val userId: String,
	val nickname: String?,
	val profileImageUrl: String?,
	val version: Long,
)
