package com.aandiclub.auth.user.domain

enum class UserRole(
	val level: Int,
) {
	USER(1),
	ORGANIZER(2),
	ADMIN(3),
}
